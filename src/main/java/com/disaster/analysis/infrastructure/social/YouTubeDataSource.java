package com.disaster.analysis.infrastructure.social;

import com.disaster.analysis.domain.exception.DataSourceException;
import com.disaster.analysis.domain.model.entities.Comment;
import com.disaster.analysis.domain.model.entities.Post;
import com.disaster.analysis.domain.model.enums.Platform;
import com.disaster.analysis.domain.contract.social.DataSource;
import com.disaster.analysis.util.LogUtil;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lớp thực thi cào dữ liệu từ YouTube thông qua Google API.
 */
public class YouTubeDataSource implements DataSource {

    private static final String APPLICATION_NAME = "Disaster Social Media Analysis"; //Tên định danh ứng dụng khi gọi lên server Google
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final int MAX_RESULTS_PER_PAGE = 50;

    private final String apiKey;
    private final YouTube youtubeService;

    public YouTubeDataSource(String apiKey) {
        this.apiKey = apiKey;

        // Triển khai dịch vụ Youtube nếu có API
        if (this.apiKey != null && !this.apiKey.trim().isEmpty()) {
            YouTube service = null;
            try {
                NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                service = new YouTube.Builder(httpTransport, JSON_FACTORY, null)
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                LogUtil.info("YouTube API client initialized with real API");
            } catch (GeneralSecurityException | IOException e) {
                LogUtil.warn("Failed to initialize YouTube API service", e);
                service = null;
            }
            this.youtubeService = service;
        } else {
            LogUtil.info("YouTube API key not configured");
            this.youtubeService = null;
        }
    }

    @Override
    public List<Post> fetchPosts(String query, LocalDateTime startDate,
                                 LocalDateTime endDate, int maxResults) throws DataSourceException {

        if (youtubeService == null) {
            LogUtil.error("YouTube service has not been initialized.");
            throw new DataSourceException(getPlatform(), "Chưa cài đặt YouTube API. Không thể cào dữ liệu.");
        }

        try {
            List<Post> posts = new ArrayList<>();
            String pageToken = null;
            LogUtil.info("Searching video on YouTube with keywords: " + query);

            while (posts.size() < maxResults) {
                long resultsNeeded = Math.min(MAX_RESULTS_PER_PAGE, maxResults - posts.size());

                YouTube.Search.List search = youtubeService.search().list(List.of("id", "snippet"));
                search.setKey(apiKey);
                search.setQ(query);
                search.setType(List.of("video"));
                search.setMaxResults(resultsNeeded);
                search.setOrder("relevance");

                if (startDate != null) search.setPublishedAfter(convertToRFC3339(startDate));
                if (endDate != null) search.setPublishedBefore(convertToRFC3339(endDate));
                if (pageToken != null) search.setPageToken(pageToken);

                SearchListResponse response = search.execute();
                if (response.getItems() == null || response.getItems().isEmpty()) {
                    LogUtil.info("No more YouTube results found");
                    break;
                }

                List<String> videoIds = response.getItems().stream()
                        .map(result -> result.getId().getVideoId())
                        .collect(Collectors.toList());

                LogUtil.info("Fetched " + videoIds.size() + " video IDs, now fetching full details...");

                List<Video> fullVideos = fetchFullVideoDetails(videoIds);
                for (Video video : fullVideos) {
                    posts.add(transformVideoToPost(video));
                }

                LogUtil.info("Fetched " + fullVideos.size() + " YouTube videos with full descriptions (total: " + posts.size() + ")");

                pageToken = response.getNextPageToken();
                if (pageToken == null) break;
            }
            return posts;
        } catch (IOException e) {
            throw new DataSourceException(getPlatform(), "Lỗi mạng khi gọi YouTube API", e);
        }
    }

    @Override
    public Platform getPlatform() {
        return Platform.YOUTUBE;
    }

    @Override
    public List<Comment> fetchComments(Post post, int maxResults) throws DataSourceException {
        if (youtubeService == null) throw new DataSourceException(getPlatform(), "Chưa cài đặt YouTube API.");

        try {
            String videoId = extractVideoId(post);
            if (videoId == null) {
                LogUtil.warn("Could not extract video ID from post: " + post.getPlatformId());
                return new ArrayList<>();
            }

            List<Comment> comments = new ArrayList<>();
            String pageToken = null;

            LogUtil.info("Fetching YouTube comments for video: " + videoId);

            while (comments.size() < maxResults) {
                long resultsNeeded = Math.min(MAX_RESULTS_PER_PAGE, maxResults - comments.size());

                YouTube.CommentThreads.List request = youtubeService.commentThreads()
                        .list(List.of("snippet", "replies"))
                        .setKey(apiKey)
                        .setVideoId(videoId)
                        .setTextFormat("plainText")
                        .setMaxResults(resultsNeeded)
                        .setOrder("relevance");

                if (pageToken != null) request.setPageToken(pageToken);

                com.google.api.services.youtube.model.CommentThreadListResponse response;
                try {
                    response = request.execute();
                } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
                    if (e.getStatusCode() == 403) {
                        if (e.getMessage().contains("commentsDisabled")) {
                            LogUtil.info("Comments are disabled for video: " + videoId);
                            return comments; // Return empty or partial list
                        } else if (e.getMessage().contains("quotaExceeded")) {
                            throw new DataSourceException(Platform.YOUTUBE,
                                    "YouTube API quota exceeded. Please try again later.", e);
                        }
                    }
                    else if (e.getStatusCode() == 404) {
                        LogUtil.warn("Video not found: " + videoId);
                        return comments;
                    }
                    throw e;
                }

                if (response.getItems() == null || response.getItems().isEmpty()) break;

                for (com.google.api.services.youtube.model.CommentThread thread : response.getItems()) {
                    comments.add(transformYouTubeCommentToComment(thread.getSnippet().getTopLevelComment(), post));
                    if (comments.size() >= maxResults) break;

                    if (thread.getReplies() != null && thread.getReplies().getComments() != null) {
                        for (com.google.api.services.youtube.model.Comment reply : thread.getReplies().getComments()) {
                            if (comments.size() >= maxResults) break;
                            comments.add(transformYouTubeCommentToComment(reply, post));
                        }
                    }
                    if (comments.size() >= maxResults) break;
                }
                pageToken = response.getNextPageToken();
                if (pageToken == null) break;
            }
            return comments;
        } catch (IOException e) {
            throw new DataSourceException(Platform.YOUTUBE, "Lỗi cào bình luận", e);
        }
    }

    /**
     * Hàm này dùng để chuyển đổi kiểu thời gian LocalDateTime của Java (giờ địa phương trên máy tính của bạn,
     * ví dụ múi giờ Việt Nam là UTC+7) thành một chuỗi văn bản (String) theo định dạng tiêu chuẩn quốc tế
     * RFC 3339 ở múi giờ gốc UTC.
     * @param dateTime
     * @return
     */
    private String convertToRFC3339(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_INSTANT);
    }

    private List<Video> fetchFullVideoDetails(List<String> videoIds) throws DataSourceException {
        if (videoIds == null || videoIds.isEmpty()) return new ArrayList<>();
        try {

            // YouTube API allows up to 50 video IDs per request
            List<Video> allVideos = new ArrayList<>();

            // Process in batches of 50
            for (int i = 0; i < videoIds.size(); i += 50) {
                int endIndex = Math.min(i + 50, videoIds.size());

                // Build videos.list request
                YouTube.Videos.List request = youtubeService.videos()
                        .list(List.of("snippet"))
                        .setId(videoIds.subList(i, endIndex))
                        .setKey(apiKey);

                // Execute request
                VideoListResponse response = request.execute();
                if (response.getItems() != null) {
                    allVideos.addAll(response.getItems());
                    LogUtil.info("Fetched full details for " + response.getItems().size() + " videos (batch " + (i/50 + 1) + ")");
                }
            }
            return allVideos;
        } catch (IOException e) {
            throw new DataSourceException(getPlatform(), "Lỗi lấy chi tiết Video", e);
        }
    }

    private Post transformVideoToPost(Video video) {
        Post post = new Post();
        post.setPlatform(Platform.YOUTUBE.name());
        post.setPlatformId("youtube_" + video.getId());
        post.setUrl("https://www.youtube.com/watch?v=" + video.getId());
        post.setContent(video.getSnippet().getTitle() + "\n\n" + video.getSnippet().getDescription());
        post.setAuthor(video.getSnippet().getChannelTitle());

        if (video.getSnippet().getPublishedAt() != null) {
            post.setPublishedAt(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(video.getSnippet().getPublishedAt().getValue()),
                    ZoneId.systemDefault()));
        }
        post.setCollectedAt(LocalDateTime.now());
        return post;
    }

    private String extractVideoId(Post post) {
        // Cách 1: Rút trích ID trực tiếp từ chuỗi platformId (Định dạng chuẩn: "youtube_VIDEO_ID")
        // Đây là cách nhanh và chính xác nhất do hệ thống tự sinh ra lúc cào dữ liệu
        if (post.getPlatformId() != null && post.getPlatformId().startsWith("youtube_")) {
            String videoId = post.getPlatformId().substring("youtube_".length());
            if (!videoId.isEmpty()) {
                return videoId;
            }
        }

        // Cách 2: Rút trích ID từ đường dẫn URL (Dùng làm phương án dự phòng nếu Cách 1 thất bại)
        if (post.getUrl() != null) {
            String url = post.getUrl();

            // Xử lý URL dạng xem video chuẩn (Ví dụ: https://www.youtube.com/watch?v=VIDEO_ID)
            if (url.contains("youtube.com/watch?v=")) {
                int startIndex = url.indexOf("v=") + 2;
                int endIndex = url.indexOf("&", startIndex); // Cắt bỏ các tham số phụ ở phía sau (nếu có)
                if (endIndex == -1) {
                    return url.substring(startIndex);
                } else {
                    return url.substring(startIndex, endIndex);
                }
            }

            // Xử lý URL dạng rút gọn khi chia sẻ (Ví dụ: https://youtu.be/VIDEO_ID)
            if (url.contains("youtu.be/")) {
                int startIndex = url.indexOf("youtu.be/") + 9;
                int endIndex = url.indexOf("?", startIndex); // Cắt bỏ tham số thời gian (VD: ?t=10s)
                if (endIndex == -1) {
                    return url.substring(startIndex);
                } else {
                    return url.substring(startIndex, endIndex);
                }
            }

            // Xử lý URL dạng nhúng iframe (Ví dụ: https://www.youtube.com/embed/VIDEO_ID)
            if (url.contains("youtube.com/embed/")) {
                int startIndex = url.indexOf("embed/") + 6;
                int endIndex = url.indexOf("?", startIndex);
                if (endIndex == -1) {
                    return url.substring(startIndex);
                } else {
                    return url.substring(startIndex, endIndex);
                }
            }
        }

        // Ghi log cảnh báo nếu không thể tìm thấy Video ID
        LogUtil.warn("Could not extract video ID from post: " + post.getPlatformId());
        return null;
    }

    private Comment transformYouTubeCommentToComment(com.google.api.services.youtube.model.Comment ytComment, Post post) {
        Comment comment = new Comment();

        // Set platform
        comment.setPlatform(Platform.YOUTUBE.name());

        // Set platform ID (comment ID)
        comment.setPlatformId("youtube_comment_" + ytComment.getId());

        // Set post ID (will be set by the service layer)
        comment.setPostId(post.getId());
        comment.setProjectId(post.getProjectId());

        // Set content (text)
        comment.setContent(ytComment.getSnippet().getTextOriginal() != null
                ? ytComment.getSnippet().getTextOriginal()
                : ytComment.getSnippet().getTextDisplay());

        // Set author
        comment.setAuthor(ytComment.getSnippet().getAuthorDisplayName());

        if (ytComment.getSnippet().getPublishedAt() != null) {
            comment.setPublishedAt(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(
                            ytComment.getSnippet().getPublishedAt().getValue()),
                            ZoneId.systemDefault()));
        }

        // Set collected timestamp
        comment.setCollectedAt(LocalDateTime.now());
        return comment;
    }
}