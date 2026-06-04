package com.disaster.analysis.infrastructure.social;

import com.disaster.analysis.domain.contract.social.DataSource;
import com.disaster.analysis.domain.exception.DataSourceException;
import com.disaster.analysis.domain.model.Comment;
import com.disaster.analysis.domain.model.Post;
import com.disaster.analysis.domain.model.enums.Platform;
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
 * Lớp thực thi kết nối và thu thập dữ liệu từ nền tảng Reddit
 */
public class RedditDataSource implements DataSource {
    private static final String BASE_URL = "https://www.reddit.com";// Hằng số URL gốc của Reddit
    private static final int PAGE_SIZE = 30;
    private static final int COMMENT_DEPTH = 2;// Độ sâu tối đa khi lấy reply lồng nhau

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private int rateLimitCounter = 0;

    public RedditDataSource() {
    }

    @Override
    public List<Post> fetchPosts(
            String query,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int maxResults
    ) throws DataSourceException {
        List<Post> results = new ArrayList<>();
        String after = null;
        try {
            while (results.size() < maxResults) { // Tiếp tục lấy trang mới chừng nào chưa đủ số bài. Vòng lặp kết thúc khi: đủ bài, hết trang, hoặc lỗi
                String url = buildUrl(query, after);
                HttpRequest request = HttpRequest.newBuilder() //Xây request theo builder pattern
                        .uri(URI.create(url))
                        .header("User-Agent", "disaster-analysis-app/1.0 (by u/academic-research)")
                        .GET()
                        .build();
                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 429) {
                    if (rateLimitCounter < 5) {
                        rateLimitCounter++;
                        Thread.sleep(3000);
                        continue; // Thử lại
                    } else {
                        throw new DataSourceException(
                                getPlatform(),
                                "Vượt quá giới hạn số lượng yêu cầu (Rate limit) của Reddit"
                        );
                    }
                }else if (response.statusCode() != 200) {
                        throw new DataSourceException(
                                getPlatform(),
                                "Lỗi kết nối HTTP từ Reddit, mã phản hồi: " + response.statusCode()
                        );
                }

                JsonNode root = mapper.readTree(response.body());
                JsonNode data = root.path("data");
                JsonNode children = data.path("children");

                if (!children.isArray() || children.isEmpty()) break;

                // Duyệt từng bài viết trong trang hiện tại
                for (JsonNode child : children) {
                    JsonNode postData = child.path("data");

                    long createdUtc = postData.path("created_utc").asLong();
                    LocalDateTime publishedAt = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(createdUtc),
                            ZoneId.systemDefault()
                    );

                    if (startDate != null && publishedAt.isBefore(startDate)) {
                        continue;
                    }

                    if (endDate != null && publishedAt.isAfter(endDate)) {
                        continue;
                    }

                    Post post = new Post();// Tạo Post entity và set từng field. platformId có prefix "reddit_" để phân biệt với ID từ YouTube hay News khi lưu vào cùng DB
                    post.setPlatform(getPlatform().name());
                    post.setPlatformId("reddit_" + postData.path("id").asText());
                    post.setCollectedAt(LocalDateTime.now());
                    post.setPublishedAt(publishedAt);
                    post.setAuthor(postData.path("author").asText("unknown"));
                    post.setUrl("https://www.reddit.com" + postData.path("permalink").asText());

                    String title = postData.path("title").asText("");
                    String body = postData.path("selftext").asText("");

                    post.setContent(title + "\n\n" + body);// Gộp cả hai thành content để phân tích sentiment trên toàn bộ văn bản
                    results.add(post);

                    if (results.size() >= maxResults) break;
                }

                after = data.path("after").isNull() ? null : data.path("after").asText();
                if (after == null) break;
            }

            return results;

        } catch(IOException | InterruptedException e) {
            throw new DataSourceException(
                    getPlatform(),
                    "Lấy dữ liệu từ Reddit thất bại: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public List<Comment> fetchComments(Post post, int maxResults) throws DataSourceException {
        List<Comment> results = new ArrayList<>();

        try {
            // Trích xuất ID bài đăng từ URL hoặc platformId
            String postId = extractPostId(post);
            if (postId == null) {
                return results;
            }
            // Xây dựng đường dẫn để lấy danh sách các bình luận (comments) của một bài viết cụ thể trên Reddit
            String commentsUrl = BASE_URL + "/comments/" + postId + ".json"
                    + "?depth=" + COMMENT_DEPTH
                    + "&limit=" + maxResults
                    + "&sort=new";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(commentsUrl))
                    .header("User-Agent", "disaster-analysis-app/1.0 (by u/academic-research)")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                if (rateLimitCounter < 5) {
                    rateLimitCounter++;
                    Thread.sleep(3000);
                    return fetchComments(post, maxResults); // Thử lại
                } else {
                    throw new DataSourceException(
                            getPlatform(),
                            "Đã vượt quá giới hạn số lượng bình luận trên Reddit"
                    );
                }
            } else if (response.statusCode() != 200) {
                throw new DataSourceException(
                        getPlatform(),
                        "Lỗi kết nối HTTP khi tải bình luận từ Reddit, mã phản hồi: " + response.statusCode()
                );
            }

            // Phân tích phản hồi - Reddit trả về một mảng gồm [thông tin bài viết, danh sách bình luận]
            JsonNode root = mapper.readTree(response.body());

            if (!root.isArray() || root.size() < 2) {
                return results;
            }
            // Phần tử thứ hai chứa các bình luận
            JsonNode commentsListing = root.get(1);
            JsonNode commentsData = commentsListing.path("data");
            JsonNode children = commentsData.path("children");

            if (!children.isArray()) {
                return results;
            }

            // Trích xuất bình luận một cách đệ quy (bao gồm cả các phản hồi lồng nhau)
            extractComments(children, post, results, maxResults, 0);

            return results;
        } catch (IOException | InterruptedException e) {
            throw new DataSourceException(
                    getPlatform(),
                    "Lỗi cào dữ liệu bình luận: " + e.getMessage(),
                    e
            );
        }
    }


    private void extractComments(JsonNode children, Post post, List<Comment> results, int maxResults, int depth) {
        if (results.size() >= maxResults || depth > COMMENT_DEPTH) {
            return; // Dừng nếu đã đủ bình luận hoặc quá sâu
        }

        for (JsonNode child : children) {
            if (results.size() >= maxResults) {
                break;
            }

            String kind = child.path("kind").asText();

            // "more" là object phân trang, không phải comment thật => Bỏ qua, không xử lý.
            if ("more".equals(kind)) {
                continue;
            }

            // Xử lý bình luận (kind = "t1")
            if ("t1".equals(kind)) {
                JsonNode commentData = child.path("data");

                // Lọc comment không có giá trị: rỗng, bị xóa bởi user, bị xóa bởi mod
                String body = commentData.path("body").asText("");
                if (body.isEmpty() || "[deleted]".equals(body) || "[removed]".equals(body)) {
                    continue;
                }

                Comment comment = new Comment();
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

                // Xử lý các bình luận lồng nhau
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
        // Cách 1: Lấy ID từ platformId đã được lưu khi fetch (format: "reddit_xxxxx")
        String platformId = post.getPlatformId();
        if (platformId != null && platformId.startsWith("reddit_")) {
            return platformId.substring(7); // Xóa tiền tố "reddit_"
        }

        // Cách 2: Trích xuất từ URL (format: https://www.reddit.com/r/subreddit/comments/xxxxx/...)
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