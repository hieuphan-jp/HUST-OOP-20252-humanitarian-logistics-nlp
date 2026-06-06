package com.disaster.analysis.infrastructure.social;

import com.disaster.analysis.domain.contract.social.DataSource;
import com.disaster.analysis.domain.exception.DataSourceException;
import com.disaster.analysis.domain.model.Comment;
import com.disaster.analysis.domain.model.Post;
import com.disaster.analysis.domain.model.enums.Platform;
import com.disaster.analysis.util.LogUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp thực thi kết nối và thu thập dữ liệu từ nền tảng Reddit.
 * Sử dụng cơ chế gọi HTTP công khai tới các endpoint JSON của Reddit để lấy bài viết và bình luận.
 * Đã được tối ưu hóa User-Agent chuẩn API Reddit để tránh lỗi 403 và 429.
 */
public class RedditDataSource implements DataSource {

    private static final String BASE_URL = "https://www.reddit.com";
    private static final int PAGE_SIZE = 30; // Số lượng bài viết tối đa mỗi trang
    private static final int COMMENT_DEPTH = 2; // Độ sâu đệ quy tối đa khi lấy bình luận lồng nhau

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // Bộ đếm xử lý lỗi gọi API quá nhanh
    private int rateLimitCounter = 0;

    public RedditDataSource() {
        LogUtil.info("RedditDataSource initialized successfully.");
    }

    @Override
    public List<Post> fetchPosts(
            String query,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int maxResults
    ) throws DataSourceException {

        LogUtil.info("Starting Reddit search scan for query: '" + query + "'");
        List<Post> results = new ArrayList<>();
        String after = null;

        try {
            while (results.size() < maxResults) {

                String url = buildUrl(query, after);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        // 1. Khai báo User-Agent chuẩn xác như Reddit yêu cầu để tránh lỗi 403 Forbidden
                        .header("User-Agent", "java:disaster-analysis-tool:v1.0.0 (by /u/academic_researcher)")
                        // 2. Yêu cầu định dạng JSON
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 429) {
                    if (rateLimitCounter < 5) {
                        rateLimitCounter++;
                        LogUtil.warn("Reddit API rate limit hit (429). Backing off and retrying in 3 seconds... Attempt: " + rateLimitCounter);
                        Thread.sleep(3000);
                        continue; // Thử lại
                    } else {
                        LogUtil.error("Reddit rate limit exceeded maximum retry threshold.");
                        throw new DataSourceException(getPlatform(), "Too many requests (Rate limit exceeded) on Reddit API.");
                    }
                } else if (response.statusCode() != 200) {
                    LogUtil.error("HTTP connection error from Reddit with status code: " + response.statusCode());
                    throw new DataSourceException(getPlatform(), "HTTP connection error from Reddit, status code: " + response.statusCode());
                }

                rateLimitCounter = 0;

                JsonNode root = mapper.readTree(response.body());
                JsonNode data = root.path("data");
                JsonNode children = data.path("children");

                if (!children.isArray() || children.isEmpty()) {
                    LogUtil.info("No more articles found on Reddit search list.");
                    break;
                }

                for (JsonNode child : children) {
                    JsonNode postData = child.path("data");

                    long createdUtc = postData.path("created_utc").asLong();
                    LocalDateTime publishedAt = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(createdUtc),
                            ZoneId.systemDefault()
                    );

                    if (startDate != null && publishedAt.isBefore(startDate)) continue;
                    if (endDate != null && publishedAt.isAfter(endDate)) continue;

                    Post post = new Post();
                    // ĐỒNG BỘ: Sử dụng chuỗi String để phù hợp với bảng Database
                    post.setPlatform(getPlatform().name());
                    post.setPlatformId("reddit_" + postData.path("id").asText());
                    post.setCollectedAt(LocalDateTime.now());
                    post.setPublishedAt(publishedAt);
                    post.setAuthor(postData.path("author").asText("unknown"));
                    post.setUrl(BASE_URL + postData.path("permalink").asText());

                    String title = postData.path("title").asText("");
                    String body = postData.path("selftext").asText("");

                    post.setContent(title + "\n\n" + body);
                    results.add(post);

                    if (results.size() >= maxResults) break;
                }

                after = data.path("after").isNull() ? null : data.path("after").asText();
                if (after == null) {
                    LogUtil.info("Reached the end of Reddit search pagination.");
                    break;
                }

                LogUtil.info("Moving to next Reddit result page using pagination token: " + after);
            }

            LogUtil.info("Reddit post fetching process completed. Total posts retrieved: " + results.size());
            return results;

        } catch (IOException | InterruptedException e) {
            LogUtil.error("Failed to fetch posts from Reddit due to system exception: " + e.getMessage());
            throw new DataSourceException(getPlatform(), "Reddit data collection failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Comment> fetchComments(Post post, int maxResults) throws DataSourceException {
        List<Comment> results = new ArrayList<>();

        try {
            String postId = extractPostId(post);
            if (postId == null) {
                LogUtil.warn("Skipping comment fetch: Unable to extract valid Reddit post ID.");
                return results;
            }

            LogUtil.info("Fetching comments for Reddit post ID: " + postId);

            String commentsUrl = BASE_URL + "/comments/" + postId + ".json"
                    + "?depth=" + COMMENT_DEPTH
                    + "&limit=" + maxResults
                    + "&sort=new";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(commentsUrl))
                    .header("User-Agent", "java:disaster-analysis-tool:v1.0.0 (by /u/academic_researcher)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                if (rateLimitCounter < 5) {
                    rateLimitCounter++;
                    LogUtil.warn("Reddit Comment API rate limit hit. Retrying in 3 seconds...");
                    Thread.sleep(3000);
                    return fetchComments(post, maxResults);
                } else {
                    LogUtil.error("Reddit comment limit retry threshold exceeded.");
                    throw new DataSourceException(getPlatform(), "Rate limit exceeded during comment collection.");
                }
            } else if (response.statusCode() != 200) {
                LogUtil.error("HTTP connection error during Reddit comment fetch, status: " + response.statusCode());
                throw new DataSourceException(getPlatform(), "HTTP connection error during comment download, status: " + response.statusCode());
            }

            rateLimitCounter = 0;

            JsonNode root = mapper.readTree(response.body());
            if (!root.isArray() || root.size() < 2) {
                return results;
            }

            JsonNode commentsListing = root.get(1);
            JsonNode commentsData = commentsListing.path("data");
            JsonNode children = commentsData.path("children");

            if (!children.isArray()) {
                return results;
            }

            extractComments(children, post, results, maxResults, 0);

            LogUtil.info("Successfully processed " + results.size() + " comments for post ID: " + post.getId());
            return results;

        } catch (IOException | InterruptedException e) {
            LogUtil.error("Error occurred while parsing Reddit comments payload: " + e.getMessage());
            throw new DataSourceException(getPlatform(), "Comment scraping execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Hàm đệ quy bóc tách chuỗi bình luận từ cây dữ liệu JSON của Reddit.
     */
    private void extractComments(JsonNode children, Post post, List<Comment> results,
                                 int maxResults, int depth) {
        if (results.size() >= maxResults || depth > COMMENT_DEPTH) {
            return;
        }

        for (JsonNode child : children) {
            if (results.size() >= maxResults) {
                break;
            }

            String kind = child.path("kind").asText();

            if ("more".equals(kind)) {
                continue;
            }

            if ("t1".equals(kind)) {
                JsonNode commentData = child.path("data");

                String body = commentData.path("body").asText("");
                if (body.isEmpty() || "[deleted]".equals(body) || "[removed]".equals(body)) {
                    continue;
                }

                Comment comment = new Comment();
                // ĐỒNG BỘ: Sử dụng .name() để đưa về chuẩn String
                comment.setPlatform(getPlatform().name());
                comment.setPlatformId("reddit_comment_" + commentData.path("id").asText());
                comment.setPostId(post.getId());
                comment.setCollectedAt(LocalDateTime.now());

                long createdUtc = commentData.path("created_utc").asLong();
                comment.setPublishedAt(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(createdUtc),
                        ZoneId.systemDefault()
                ));

                comment.setAuthor(commentData.path("author").asText("unknown"));
                comment.setContent(body);

                results.add(comment);

                JsonNode replies = commentData.path("replies");
                if (replies.isObject()) {
                    JsonNode repliesData = replies.path("data");
                    JsonNode repliesChildren = repliesData.path("children");
                    if (repliesChildren.isArray()) {
                        extractComments(repliesChildren, post, results, maxResults, depth + 1);
                    }
                }
            }
        }
    }

    private String extractPostId(Post post) {
        String platformId = post.getPlatformId();
        if (platformId != null && platformId.startsWith("reddit_")) {
            return platformId.substring(7);
        }

        String url = post.getUrl();
        if (url != null && url.contains("/comments/")) {
            String[] parts = url.split("/comments/");
            if (parts.length > 1) {
                String[] idParts = parts[1].split("/");
                if (idParts.length > 0) {
                    return idParts[0];
                }
            }
        }

        return null;
    }

    @Override
    public Platform getPlatform() {
        return Platform.REDDIT;
    }

    private String buildUrl(String query, String after) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        StringBuilder url = new StringBuilder(BASE_URL).append("/search.json")
                .append("?q=").append(encodedQuery)
                .append("&sort=new")
                .append("&limit=").append(PAGE_SIZE);

        if (after != null) {
            url.append("&after=").append(after);
        }
        return url.toString();
    }
}