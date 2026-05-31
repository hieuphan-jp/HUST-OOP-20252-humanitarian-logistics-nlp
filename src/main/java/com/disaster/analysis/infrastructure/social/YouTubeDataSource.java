package com.disaster.analysis.infrastructure.social;

import com.disaster.analysis.domain.exception.DataSourceException;
import com.disaster.analysis.domain.model.Comment;
import com.disaster.analysis.domain.model.Post;
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

    private static final String APPLICATION_NAME = "Disaster Social Media Analysis";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final int MAX_RESULTS_PER_PAGE = 50;

    private final String apiKey;
    private final YouTube youtubeService;

    public YouTubeDataSource(String apiKey) {
        this.apiKey = apiKey;

        if (this.apiKey != null && !this.apiKey.trim().isEmpty()) {
            YouTube service = null;
            try {
                NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                service = new YouTube.Builder(httpTransport, JSON_FACTORY, null)
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                LogUtil.info("YouTube API Client đã được khởi tạo thành công.");
            } catch (GeneralSecurityException | IOException e) {
                LogUtil.warn("Không thể khởi tạo YouTube API", e);
                service = null;
            }
            this.youtubeService = service;
        } else {
            LogUtil.info("Chưa cấu hình YouTube API Key.");
            this.youtubeService = null;
        }
    }

    @Override
    public List<Post> fetchPosts(String query, LocalDateTime startDate,
                                 LocalDateTime endDate, int maxResults) throws DataSourceException {

        if (youtubeService == null) {
            throw new DataSourceException(getPlatform(), "Chưa cài đặt YouTube API. Không thể cào dữ liệu.");
        }

        try {
            List<Post> posts = new ArrayList<>();
            String pageToken = null;
            LogUtil.info("Đang tìm kiếm Video YouTube với từ khóa: " + query);

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
                if (response.getItems() == null || response.getItems().isEmpty()) break;

                List<String> videoIds = response.getItems().stream()
                        .map(result -> result.getId().getVideoId())
                        .collect(Collectors.toList());

                List<Video> fullVideos = fetchFullVideoDetails(videoIds);
                for (Video video : fullVideos) {
                    posts.add(transformVideoToPost(video));
                }

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

    private String convertToRFC3339(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT);
    }

    private List<Video> fetchFullVideoDetails(List<String> videoIds) throws DataSourceException {
        if (videoIds == null || videoIds.isEmpty()) return new ArrayList<>();
        try {
            List<Video> allVideos = new ArrayList<>();
            for (int i = 0; i < videoIds.size(); i += 50) {
                int endIndex = Math.min(i + 50, videoIds.size());
                YouTube.Videos.List request = youtubeService.videos()
                        .list(List.of("snippet"))
                        .setId(videoIds.subList(i, endIndex))
                        .setKey(apiKey);
                VideoListResponse response = request.execute();
                if (response.getItems() != null) allVideos.addAll(response.getItems());
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

    @Override
    public List<Comment> fetchComments(Post post, int maxResults) throws DataSourceException {
        if (youtubeService == null) throw new DataSourceException(getPlatform(), "Chưa cài đặt YouTube API.");

        try {
            String videoId = extractVideoId(post);
            if (videoId == null) return new ArrayList<>();

            List<Comment> comments = new ArrayList<>();
            String pageToken = null;

            while (comments.size() < maxResults) {
                YouTube.CommentThreads.List request = youtubeService.commentThreads()
                        .list(List.of("snippet", "replies"))
                        .setKey(apiKey)
                        .setVideoId(videoId)
                        .setTextFormat("plainText")
                        .setMaxResults(Math.min(MAX_RESULTS_PER_PAGE, maxResults - comments.size()))
                        .setOrder("relevance");

                if (pageToken != null) request.setPageToken(pageToken);

                com.google.api.services.youtube.model.CommentThreadListResponse response;
                try {
                    response = request.execute();
                } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
                    if (e.getStatusCode() == 403 && e.getMessage().contains("commentsDisabled")) return comments;
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

    private String extractVideoId(Post post) {
        if (post.getPlatformId() != null && post.getPlatformId().startsWith("youtube_")) {
            return post.getPlatformId().substring(8);
        }
        return null;
    }

    private Comment transformYouTubeCommentToComment(com.google.api.services.youtube.model.Comment ytComment, Post post) {
        Comment comment = new Comment();
        comment.setPlatform(Platform.YOUTUBE.name());
        comment.setPlatformId("youtube_comment_" + ytComment.getId());
        comment.setPostId(post.getId());
        comment.setProjectId(post.getProjectId());
        comment.setContent(ytComment.getSnippet().getTextOriginal() != null ? ytComment.getSnippet().getTextOriginal() : ytComment.getSnippet().getTextDisplay());
        comment.setAuthor(ytComment.getSnippet().getAuthorDisplayName());

        if (ytComment.getSnippet().getPublishedAt() != null) {
            comment.setPublishedAt(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(ytComment.getSnippet().getPublishedAt().getValue()), ZoneId.systemDefault()));
        }
        comment.setCollectedAt(LocalDateTime.now());
        return comment;
    }
}