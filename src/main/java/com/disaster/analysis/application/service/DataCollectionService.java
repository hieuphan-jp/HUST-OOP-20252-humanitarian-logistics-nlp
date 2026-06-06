package com.disaster.analysis.application.service;

import com.disaster.analysis.domain.contract.social.DataSource;
import com.disaster.analysis.domain.exception.DataSourceException;
import com.disaster.analysis.domain.model.Comment;
import com.disaster.analysis.domain.model.Post;
import com.disaster.analysis.domain.model.Project;
import com.disaster.analysis.domain.model.enums.Platform;
import com.disaster.analysis.domain.contract.repository.CommentRepository;
import com.disaster.analysis.domain.contract.repository.PostRepository;
import com.disaster.analysis.domain.contract.repository.ProjectRepository;
import com.disaster.analysis.util.LogUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service đảm nhiệm việc điều phối thu thập dữ liệu từ nhiều nguồn mạng xã hội khác nhau.
 * Hoạt động theo cơ chế Đa luồng (Multi-threading) để tối ưu hóa tốc độ cào dữ liệu.
 * Đã được đồng bộ hoàn toàn với cấu trúc Project lưu trữ chuỗi phẳng phân tách bằng dấu phẩy.
 */
public class DataCollectionService {

    private final Map<Platform, DataSource> dataSources;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PreprocessingService preprocessingService;
    private final ProjectRepository projectRepository;

    // Các hằng số cấu hình hệ thống
    private static final int DEFAULT_MAX_RESULTS_PER_PLATFORM = 50;
    private static final int DEFAULT_MAX_COMMENTS_PER_POST = 50;
    private static final int THREAD_POOL_SIZE = 3; // Giới hạn số luồng chạy song song
    private static final long COLLECTION_TIMEOUT_MINUTES = 10; // Thời gian chờ hủy luồng

    public DataCollectionService(PostRepository postRepository,
                                 CommentRepository commentRepository,
                                 PreprocessingService preprocessingService,
                                 Map<Platform, DataSource> dataSources,
                                 ProjectRepository projectRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.preprocessingService = preprocessingService;
        this.projectRepository = projectRepository;
        this.dataSources = new HashMap<>(dataSources);

        LogUtil.info("Initialized API clients for " + this.dataSources.size() + " platforms");
    }

    /**
     * Hàm lõi điều phối toàn bộ chuỗi tiến trình cào Bài viết và Bình luận của một Dự án.
     * Tự động băm nhỏ cấu hình chuỗi từ DB ra để cung cấp cho bộ lọc làm sạch dữ liệu.
     * * @param project Đối tượng dự án (chứa cấu hình từ khóa, nền tảng dạng chuỗi cách nhau bởi dấu phẩy).
     * @param progressCallback Hàm callback dùng để đẩy phần trăm tiến độ thời gian thực lên giao diện UI.
     */
    public void collectData(Project project, Consumer<Integer> progressCallback) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }

        LogUtil.info("Starting data collection for project: " + project.getName());
        reportProgress(progressCallback, 0);

        // Ghép chuỗi từ khóa và hashtag thô thành một câu truy vấn tìm kiếm duy nhất
        String query = buildSearchQuery(project);
        LogUtil.info("Search query constructed: " + query);

        // Thông dịch thuộc tính chuỗi project.getPlatforms() thành Set<Platform> để chạy đa luồng
        Set<Platform> platforms = parsePlatforms(project.getPlatforms());
        if (platforms.isEmpty()) {
            throw new RuntimeException("No platforms selected or configured for collection");
        }

        // Khởi tạo Executor Service quản lý các luồng chạy song song
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(THREAD_POOL_SIZE, platforms.size())
        );

        try {
            Map<Platform, Future<List<Post>>> futures = new HashMap<>();

            // Phân phối tác vụ cào dữ liệu cho từng nền tảng chạy trên luồng biệt lập
            for (Platform platform : platforms) {
                DataSource client = dataSources.get(platform);
                if (client == null) {
                    LogUtil.warn("No API client available for platform: " + platform);
                    continue;
                }

                Future<List<Post>> future = executor.submit(() ->
                        collectFromPlatform(client, query, project)
                );
                futures.put(platform, future);
            }

            reportProgress(progressCallback, 20);

            List<Post> allPosts = new ArrayList<>();
            Map<Platform, Exception> platformErrors = new HashMap<>();
            int completedPlatforms = 0;

            // Lần lượt thu hồi kết quả thu thập từ đường ống của các luồng
            for (Map.Entry<Platform, Future<List<Post>>> entry : futures.entrySet()) {
                Platform platform = entry.getKey();
                Future<List<Post>> future = entry.getValue();

                try {
                    List<Post> posts = future.get(COLLECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                    allPosts.addAll(posts);
                    LogUtil.info("Collected " + posts.size() + " posts from " + platform);
                } catch (TimeoutException e) {
                    LogUtil.warn("Collection from " + platform + " timed out after limit");
                    platformErrors.put(platform, e);
                    future.cancel(true);
                } catch (Exception e) {
                    LogUtil.error("Collection from " + platform + " failed", e);
                    platformErrors.put(platform, e);
                }

                completedPlatforms++;
                int progress = 20 + (int) ((completedPlatforms / (double) futures.size()) * 30);
                reportProgress(progressCallback, progress);
            }

            if (allPosts.isEmpty() && !platformErrors.isEmpty()) {
                throw new RuntimeException("All configured platforms failed during execution.");
            }

            LogUtil.info("Total raw posts collected: " + allPosts.size());
            reportProgress(progressCallback, 50);

            // Băm chuỗi project.getKeywords() thành mảng List<String> cấp cho PreprocessingService
            List<String> keywordList = parseCommaSeparatedString(project.getKeywords());

            // Tiến hành lọc trùng lặp và làm sạch văn bản thô
            List<Post> validPosts = preprocessingService.filterAndPreprocess(allPosts, keywordList);
            LogUtil.info("Valid posts available after preprocessing: " + validPosts.size());
            reportProgress(progressCallback, 60);

            validPosts.forEach(post -> post.setProjectId(project.getId()));

            // Lưu hàng loạt bài viết xuống DB để SQL sinh ra các ID tự tăng
            if (!validPosts.isEmpty()) {
                int savedCount = postRepository.saveBatch(validPosts);
                LogUtil.info("Saved " + savedCount + " posts to database");

                // Nạp ngược lại từ DB lên để lấy danh sách bài viết đã chứa ID thật
                validPosts = postRepository.findByProjectId(project.getId());
                LogUtil.info("Reloaded " + validPosts.size() + " posts equipped with database IDs");
            }

            reportProgress(progressCallback, 70);

            // BẮT ĐẦU GIAI ĐOẠN 2: Đi sâu thu thập bình luận cho từng bài viết hợp lệ
            List<Comment> allComments = new ArrayList<>();
            int processedPosts = 0;

            for (Post post : validPosts) {
                try {
                    if (post.getId() == null) {
                        LogUtil.warn("Skipping comment collection for post missing ID: " + post.getPlatformId());
                        continue;
                    }

                    DataSource client = dataSources.get(Platform.valueOf(post.getPlatform()));
                    if (client != null) {
                        List<Comment> comments = collectCommentsForPost(post, client);
                        allComments.addAll(comments);
                        LogUtil.info("Collected " + comments.size() + " comments for post ID: " + post.getId());
                    }
                } catch (Exception e) {
                    LogUtil.warn("Failed to retrieve comments for specific post: " + e.getMessage());
                }

                processedPosts++;
                int progress = 70 + (int) ((processedPosts / (double) validPosts.size()) * 20);
                reportProgress(progressCallback, progress);
            }

            LogUtil.info("Total raw comments collected: " + allComments.size());
            reportProgress(progressCallback, 90);

            // Làm sạch dữ liệu bình luận
            List<Comment> validComments = preprocessingService.filterAndPreprocessComments(allComments, keywordList);
            LogUtil.info("Valid comments available after preprocessing: " + validComments.size());

            // Lưu hàng loạt bình luận xuống cơ sở dữ liệu
            if (!validComments.isEmpty()) {
                int savedCount = commentRepository.saveBatch(validComments);
                LogUtil.info("Saved " + savedCount + " comments to database");
            }

            reportProgress(progressCallback, 100);

            // Tự động quét dòng thời gian để cập nhật ngày bắt đầu/kết thúc cho dự án
            updateProjectDatesFromCollectedData(project, validPosts, validComments);

        } // Sửa lỗi cú pháp thiếu khối catch từ file cũ
        catch (Exception e) {
            LogUtil.error("Critical error inside data collection pipeline", e);
            throw new RuntimeException(e);
        } finally {
            // Tắt hoàn toàn bộ quản lý luồng, ngăn chặn hiện tượng rò rỉ bộ nhớ (Memory Leak)
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Triển khai tác vụ cào bài viết độc lập cho một Driver mạng xã hội.
     */
    private List<Post> collectFromPlatform(DataSource client, String query, Project project) {
        Platform platform = client.getPlatform();
        LogUtil.info("Async thread started collection from: " + platform);

        try {
            List<Post> posts = client.fetchPosts(
                    query,
                    project.getStartDate(),
                    project.getEndDate(),
                    DEFAULT_MAX_RESULTS_PER_PLATFORM
            );

            posts.forEach(post -> {
                post.setProjectId(project.getId());
                post.setPlatform(platform.name()); // Khớp hoàn toàn với trường String platform của Post mới
            });

            return posts;
        } catch (DataSourceException e) {
            throw new RuntimeException("Data fetch error on platform " + platform + ": " + e.getMessage(), e);
        }
    }

    /**
     * Triển khai tác vụ bóc tách bình luận của một bài đăng cụ thể.
     */
    private List<Comment> collectCommentsForPost(Post post, DataSource client) throws DataSourceException {
        try {
            List<Comment> comments = client.fetchComments(post, DEFAULT_MAX_COMMENTS_PER_POST);

            comments.forEach(comment -> {
                comment.setPostId(post.getId());
                comment.setProjectId(post.getProjectId());
                comment.setPlatform(post.getPlatform());
            });

            return comments;
        } catch (DataSourceException e) {
            LogUtil.warn("Error collecting comments for platform target: " + post.getPlatformId());
            throw e;
        }
    }

    /**
     * Bóc tách câu chữ từ chuỗi phân cách dấu phẩy của dự án để tạo thành câu lệnh Search tổng hợp.
     */
    private String buildSearchQuery(Project project) {
        List<String> queryParts = new ArrayList<>();

        if (project.getKeywords() != null && !project.getKeywords().trim().isEmpty()) {
            Arrays.stream(project.getKeywords().split(","))
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .forEach(queryParts::add);
        }

        if (project.getHashtags() != null && !project.getHashtags().trim().isEmpty()) {
            Arrays.stream(project.getHashtags().split(","))
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .map(tag -> tag.startsWith("#") ? tag : "#" + tag)
                    .forEach(queryParts::add);
        }

        return String.join(" ", queryParts);
    }

    /**
     * Kẹp giá trị an toàn và đẩy phần trăm tiến độ về cho giao diện JavaFX hiển thị.
     */
    private void reportProgress(Consumer<Integer> callback, int progress) {
        if (callback != null) {
            callback.accept(Math.min(100, Math.max(0, progress)));
        }
    }

    /**
     * Hàm tiện ích băm nhỏ chuỗi thô ngăn cách bởi dấu phẩy thành List các chuỗi đơn lẻ.
     */
    private List<String> parseCommaSeparatedString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Hàm tiện ích thông dịch chuỗi cấu hình "YOUTUBE,NEWS" từ DB thành một Set<Platform> để chạy đa luồng.
     */
    private Set<Platform> parsePlatforms(String platformsStr) {
        Set<Platform> platforms = new HashSet<>();
        if (platformsStr != null && !platformsStr.trim().isEmpty()) {
            for (String p : platformsStr.split(",")) {
                try {
                    platforms.add(Platform.valueOf(p.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    LogUtil.warn("Unknown platform enum type configured in system: " + p);
                }
            }
        }
        return platforms;
    }

    /**
     * Quét dòng thời gian thực tế của bài viết/bình luận thu thập được để tự động điền mốc ngày bắt đầu/kết thúc dự án.
     */
    private void updateProjectDatesFromCollectedData(Project project, List<Post> posts, List<Comment> comments) {
        if (posts.isEmpty() && comments.isEmpty()) {
            LogUtil.info("No timeline data found, skip date adjusting");
            return;
        }

        LocalDateTime earliestDate = null;
        LocalDateTime latestDate = null;

        for (Post post : posts) {
            if (post.getPublishedAt() != null) {
                if (earliestDate == null || post.getPublishedAt().isBefore(earliestDate)) earliestDate = post.getPublishedAt();
                if (latestDate == null || post.getPublishedAt().isAfter(latestDate)) latestDate = post.getPublishedAt();
            }
        }

        for (Comment comment : comments) {
            if (comment.getPublishedAt() != null) {
                if (earliestDate == null || comment.getPublishedAt().isBefore(earliestDate)) earliestDate = comment.getPublishedAt();
                if (latestDate == null || comment.getPublishedAt().isAfter(latestDate)) latestDate = comment.getPublishedAt();
            }
        }

        boolean updated = false;
        if (project.getStartDate() == null && earliestDate != null) {
            project.setStartDate(earliestDate);
            updated = true;
            LogUtil.info("Auto-adjusted project start date to: " + earliestDate);
        }

        if (project.getEndDate() == null && latestDate != null) {
            project.setEndDate(latestDate);
            updated = true;
            LogUtil.info("Auto-adjusted project end date to: " + latestDate);
        }

        if (updated) {
            try {
                projectRepository.update(project);
                LogUtil.info("Project timeline saved successfully into database config.");
            } catch (Exception e) {
                LogUtil.warn("Database failed to update project timeline parameters: " + e.getMessage());
            }
        }
    }
}