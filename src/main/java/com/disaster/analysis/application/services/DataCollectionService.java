package com.disaster.analysis.application.service;

import com.disaster.analysis.application.dto.ProjectDTO;
import com.disaster.analysis.domain.contract.analysis.DamageClassifier;
import com.disaster.analysis.domain.contract.analysis.SentimentAnalyzer;
import com.disaster.analysis.domain.contract.preprocessing.TextPreprocessor;
import com.disaster.analysis.domain.contract.repository.CommentRepository;
import com.disaster.analysis.domain.contract.repository.PostRepository;
import com.disaster.analysis.domain.contract.repository.ProjectRepository;
import com.disaster.analysis.domain.contract.social.DataSource;
import com.disaster.analysis.domain.exception.DataSourceException;
import com.disaster.analysis.domain.model.Comment;
import com.disaster.analysis.domain.model.Post;
import com.disaster.analysis.domain.model.Project;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Platform;
import com.disaster.analysis.util.LogUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Dịch vụ thu thập dữ liệu cốt lõi (Đa luồng).
 * Luồng chạy: Cào dữ liệu -> Làm sạch (Preprocess) -> Phân tích Cảm xúc -> Phân loại Thiệt hại -> Lưu Database.
 */
public class DataCollectionService {

    private final Map<Platform, DataSource> dataSources;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ProjectRepository projectRepository;

    // Các công cụ NLP được tiêm trực tiếp vào đây thay vì qua lớp trung gian
    private final TextPreprocessor textPreprocessor;
    private final SentimentAnalyzer sentimentAnalyzer;
    private final DamageClassifier damageClassifier;

    private static final int DEFAULT_MAX_RESULTS_PER_PLATFORM = 50;
    private static final int DEFAULT_MAX_COMMENTS_PER_POST = 50;
    private static final int THREAD_POOL_SIZE = 3;
    private static final long COLLECTION_TIMEOUT_MINUTES = 10;

    public DataCollectionService(PostRepository postRepository,
                                 CommentRepository commentRepository,
                                 ProjectRepository projectRepository,
                                 TextPreprocessor textPreprocessor,
                                 SentimentAnalyzer sentimentAnalyzer,
                                 DamageClassifier damageClassifier,
                                 Map<Platform, DataSource> dataSources) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.projectRepository = projectRepository;
        this.textPreprocessor = textPreprocessor;
        this.sentimentAnalyzer = sentimentAnalyzer;
        this.damageClassifier = damageClassifier;
        this.dataSources = dataSources;
    }

    /**
     * Hàm chính: Thu thập dữ liệu giao tiếp với giao diện thông qua ProjectDTO
     */
    public void collectData(ProjectDTO projectDto, Consumer<Integer> progressCallback) {
        if (projectDto == null || projectDto.getId() == null) {
            throw new IllegalArgumentException("Dự án không hợp lệ.");
        }

        // 1. Lấy Entity gốc từ DB lên để cập nhật
        Project project = projectRepository.findById(projectDto.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dự án trong Database!"));

        LogUtil.info("Bắt đầu thu thập dữ liệu cho dự án: " + projectDto.getName());
        reportProgress(progressCallback, 0);

        String query = buildSearchQuery(projectDto);
        Set<Platform> platforms = projectDto.getPlatforms();

        if (platforms == null || platforms.isEmpty()) {
            throw new RuntimeException("Chưa chọn mạng xã hội để cào dữ liệu.");
        }

        // 2. Khởi tạo Đa luồng (Multi-threading)
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(THREAD_POOL_SIZE, platforms.size()));

        try {
            Map<Platform, Future<List<Post>>> futures = new HashMap<>();

            // 3. Ném các tác vụ cào Post vào luồng chạy song song
            for (Platform platform : platforms) {
                DataSource client = dataSources.get(platform);
                if (client != null) {
                    futures.put(platform, executor.submit(() -> collectFromPlatform(client, query, projectDto)));
                }
            }
            reportProgress(progressCallback, 20);

            List<Post> validPosts = new ArrayList<>();
            int completedPlatforms = 0;

            // 4. Thu thập kết quả Bài viết (Post) và XỬ LÝ NLP NGAY LẬP TỨC
            for (Map.Entry<Platform, Future<List<Post>>> entry : futures.entrySet()) {
                try {
                    List<Post> rawPosts = entry.getValue().get(COLLECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                    for (Post post : rawPosts) {
                        if (!postRepository.existsByPlatformId(post.getPlatformId())) {
                            processAndAnalyzePost(post); // Gọi hàm NLP
                            validPosts.add(post);
                        }
                    }
                } catch (Exception e) {
                    LogUtil.error("Lỗi cào dữ liệu từ nền tảng " + entry.getKey().name(), e);
                }
                completedPlatforms++;
                reportProgress(progressCallback, 20 + (int) ((completedPlatforms / (double) futures.size()) * 30));
            }

            // 5. Lưu Post xuống SQL Server bằng Batch (Lưu hàng loạt)
            if (!validPosts.isEmpty()) {
                postRepository.saveBatch(validPosts);
                LogUtil.info("Đã lưu " + validPosts.size() + " bài viết.");
            }
            reportProgress(progressCallback, 60);

            // 6. Cào Bình luận (Comment) dựa trên các Post vừa lưu
            List<Comment> validComments = new ArrayList<>();
            int processedPosts = 0;

            // BẮT BUỘC: Lấy lại Post từ DB để có ID tự tăng của SQL Server
            List<Post> savedPosts = postRepository.findByProjectId(project.getId());

            for (Post post : savedPosts) {
                try {
                    Platform p = Platform.valueOf(post.getPlatform());
                    DataSource client = dataSources.get(p);
                    if (client != null) {
                        List<Comment> rawComments = client.fetchComments(post, DEFAULT_MAX_COMMENTS_PER_POST);
                        for (Comment c : rawComments) {
                            if (!commentRepository.existsByPlatformId(c.getPlatformId())) {
                                processAndAnalyzeComment(c); // Gọi hàm NLP
                                validComments.add(c);
                            }
                        }
                    }
                } catch (Exception e) {
                    LogUtil.warn("Không thể lấy bình luận cho bài viết ID: " + post.getId());
                }
                processedPosts++;
                reportProgress(progressCallback, 60 + (int) ((processedPosts / (double) savedPosts.size()) * 30));
            }

            // 7. Lưu Comment xuống SQL Server
            if (!validComments.isEmpty()) {
                commentRepository.saveBatch(validComments);
                LogUtil.info("Đã lưu " + validComments.size() + " bình luận.");
            }
            reportProgress(progressCallback, 100);

            // 8. Cập nhật ngày tháng dự án dựa trên dữ liệu mới cào
            updateProjectDatesFromCollectedData(project, savedPosts, validComments);

            // 9. ĐỒNG BỘ NGƯỢC LÊN DTO ĐỂ GIAO DIỆN TỰ CẬP NHẬT
            projectDto.setStartDate(project.getStartDate());
            projectDto.setEndDate(project.getEndDate());
            projectDto.setLastModified(project.getLastModified());

            LogUtil.info("Hoàn tất quy trình thu thập và phân tích dữ liệu 100%!");

        } finally {
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
     * Tích hợp toàn bộ nghiệp vụ Làm sạch & Phân tích vào 1 hàm duy nhất cho Bài viết.
     */
    private void processAndAnalyzePost(Post post) {
        // 1. Làm sạch văn bản thô
        String cleanContent = textPreprocessor.preprocess(post.getContent());
        post.setPreprocessedContent(cleanContent);

        // 2. Chấm điểm cảm xúc (Tích cực, Tiêu cực, Trung lập)
        post.setSentiment(sentimentAnalyzer.analyzeSentiment(cleanContent));

        // 3. Phân loại thiệt hại (Ép kiểu Set<Enum> thành chuỗi String lưu Database)
        String damageStr = damageClassifier.classifyDamage(cleanContent).stream()
                .map(DamageCategory::name)
                .collect(Collectors.joining(","));
        post.setDamageCategories(damageStr);
    }

    /**
     * Tích hợp toàn bộ nghiệp vụ Làm sạch & Phân tích vào 1 hàm duy nhất cho Bình luận.
     */
    private void processAndAnalyzeComment(Comment comment) {
        // 1. Làm sạch văn bản thô
        String cleanContent = textPreprocessor.preprocess(comment.getContent());
        comment.setPreprocessedContent(cleanContent);

        // 2. Chấm điểm cảm xúc (Tích cực, Tiêu cực, Trung lập)
        comment.setSentiment(sentimentAnalyzer.analyzeSentiment(cleanContent));

        // 3. Phân loại thiệt hại (Ép kiểu Set<Enum> thành chuỗi String lưu Database)
        String damageStr = damageClassifier.classifyDamage(cleanContent).stream()
                .map(DamageCategory::name)
                .collect(Collectors.joining(","));
        comment.setDamageCategories(damageStr);
    }

    private List<Post> collectFromPlatform(DataSource client, String query, ProjectDTO projectDto) throws DataSourceException {
        List<Post> posts = client.fetchPosts(query, projectDto.getStartDate(), projectDto.getEndDate(), DEFAULT_MAX_RESULTS_PER_PLATFORM);
        posts.forEach(post -> {
            post.setProjectId(projectDto.getId());
            post.setPlatform(client.getPlatform().name()); // Lưu Enum dưới dạng String
        });
        return posts;
    }

    /**
     * Tự động xây dựng chuỗi truy vấn (query string) tìm kiếm dựa trên thông tin cấu hình của dự án.
     * Hàm sẽ lấy các từ khóa và hashtag, chuẩn hóa chúng rồi ghép lại thành một câu truy vấn duy nhất.
     *
     * @param projectDto Đối tượng DTO chứa thông tin dự án (từ khóa, hashtags).
     * @return Một chuỗi String chứa tất cả từ khóa và hashtag được nối với nhau bằng khoảng trắng.
     */
    private String buildSearchQuery(ProjectDTO projectDto) {
        // Khởi tạo danh sách tạm để chứa các thành phần của câu truy vấn
        List<String> queryParts = new ArrayList<>();

        // Nếu dự án có thiết lập từ khóa thường, thêm toàn bộ vào danh sách
        if (projectDto.getKeywords() != null) {
            queryParts.addAll(projectDto.getKeywords());
        }

        // Nếu dự án có thiết lập hashtag, tiến hành chuẩn hóa trước khi thêm
        if (projectDto.getHashtags() != null) {
            queryParts.addAll(projectDto.getHashtags().stream()
                    // Kiểm tra từng thẻ, nếu chưa có dấu '#' ở đầu thì tự động gắn thêm dấu '#' vào
                    .map(tag -> tag.startsWith("#") ? tag : "#" + tag)
                    .toList());
        }

        // Ghép tất cả các phần tử trong danh sách lại với nhau, phân cách bằng một khoảng trắng.
        // Ví dụ kết quả: "Bão Yagi #LuLut #ThienTai"
        return String.join(" ", queryParts);
    }


    /**
     * Gọi callback để cập nhật thanh tiến trình (progress bar) trên giao diện người dùng (UI).
     * Hàm này được thiết kế an toàn (Defensive programming) để đảm bảo tiến trình không bao giờ bị lỗi hiển thị.
     *
     * @param callback Hàm callback (hứng sự kiện) từ UI truyền xuống. Có thể null nếu chạy ngầm.
     * @param progress Phần trăm tiến trình hiện tại truyền vào.
     */
    private void reportProgress(Consumer<Integer> callback, int progress) {
        if (callback != null) {
            // Thuật toán kẹp giá trị (Clamp):
            // 1. Math.max(0, progress): Nếu progress bị âm, nó sẽ lấy số 0 (không bao giờ < 0%).
            // 2. Math.min(100, ...): Nếu progress vọt quá 100, nó sẽ lấy số 100 (không bao giờ > 100%).
            // Sau khi chốt được con số an toàn, đẩy về cho giao diện (UI) thông qua .accept()
            callback.accept(Math.min(100, Math.max(0, progress)));
        }
    }


    /**
     * Tự động dò quét và cập nhật ngày bắt đầu (startDate) / ngày kết thúc (endDate) của dự án
     * dựa trên dòng thời gian thực tế của các bài viết vừa thu thập được.
     * Tính năng này giúp dự án bao quát chính xác khoảng thời gian xảy ra thảm họa.
     *
     * @param project  Thực thể Dự án cần được cập nhật ngày tháng.
     * @param posts    Danh sách bài viết vừa cào về thành công.
     * @param comments Danh sách bình luận (Lưu ý: Đoạn code hiện tại chưa dùng biến này để dò tìm ngày).
     */
    private void updateProjectDatesFromCollectedData(Project project, List<Post> posts, List<Comment> comments) {
        LocalDateTime earliestDate = null; // Biến lưu ngày đăng bài cũ nhất (Sớm nhất)
        LocalDateTime latestDate = null;   // Biến lưu ngày đăng bài mới nhất (Muộn nhất)

        // Duyệt qua toàn bộ danh sách bài viết để tìm ra hai cột mốc thời gian lớn nhất và nhỏ nhất
        for (Post post : posts) {
            if (post.getPublishedAt() != null) {
                // Nếu earliestDate chưa có giá trị, hoặc ngày của bài viết hiện tại < earliestDate -> Cập nhật lại
                if (earliestDate == null || post.getPublishedAt().isBefore(earliestDate)) {
                    earliestDate = post.getPublishedAt();
                }
                // Nếu latestDate chưa có giá trị, hoặc ngày của bài viết hiện tại > latestDate -> Cập nhật lại
                if (latestDate == null || post.getPublishedAt().isAfter(latestDate)) {
                    latestDate = post.getPublishedAt();
                }
            }
        }

        boolean updated = false; // Cờ (flag) dùng để đánh dấu xem Dự án có thực sự bị thay đổi hay không

        // Chỉ cập nhật ngày bắt đầu cho dự án nếu người dùng chưa thiết lập (đang bằng null)
        if (project.getStartDate() == null && earliestDate != null) {
            project.setStartDate(earliestDate);
            updated = true;
        }

        // Chỉ cập nhật ngày kết thúc cho dự án nếu người dùng chưa thiết lập (đang bằng null)
        if (project.getEndDate() == null && latestDate != null) {
            project.setEndDate(latestDate);
            updated = true;
        }

        // Nếu dự án có sự thay đổi về ngày tháng, gọi Repository để lưu cập nhật thẳng xuống Database (SQL Server)
        if (updated) {
            projectRepository.update(project);
        }
    }
}