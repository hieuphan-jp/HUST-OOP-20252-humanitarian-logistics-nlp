package com.disaster.analysis.application.service;

import com.disaster.analysis.application.dto.AISummaryDTO;
import com.disaster.analysis.application.mapper.AISummaryMapper;
import com.disaster.analysis.domain.contract.ai.AIClient;
import com.disaster.analysis.domain.exception.AIClientException;
import com.disaster.analysis.domain.model.entities.AISummary;
import com.disaster.analysis.domain.model.entities.Comment;
import com.disaster.analysis.domain.model.entities.Post;
import com.disaster.analysis.domain.model.entities.Project;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Sentiment;
import com.disaster.analysis.domain.contract.repository.CommentRepository;
import com.disaster.analysis.domain.contract.repository.PostRepository;
import com.disaster.analysis.domain.contract.repository.ProjectRepository;
import com.disaster.analysis.domain.contract.repository.SummaryRepository;
import com.disaster.analysis.util.LogUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dịch vụ chuyên trách giao tiếp với AI (Gemini/OpenAI) để tổng hợp báo cáo.
 * Có thể được gọi độc lập khi người dùng muốn "Tạo lại báo cáo" mà không cần cào lại dữ liệu.
 */
public class AISummaryService {

    private final AIClient aiClient;
    private final SummaryRepository summaryRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ProjectRepository projectRepository;

    // Cấu hình giới hạn số lượng bài viết gửi cho AI (Tránh vượt quá token limit)
    private static final int MAX_POSTS = 15;
    private static final int MAX_COMMENTS = 15;

    public AISummaryService(AIClient aiClient,
                            SummaryRepository summaryRepository,
                            PostRepository postRepository,
                            CommentRepository commentRepository,
                            ProjectRepository projectRepository) {
        this.aiClient = aiClient;
        this.summaryRepository = summaryRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.projectRepository = projectRepository;

        LogUtil.info("Initialized API clients for " + this.aiClient.getModelName() + " successfully");
    }

    /**
     * Lấy báo cáo AI đã lưu trong Database (Nếu có).
     */
    public Optional<AISummaryDTO> getExistingSummary(Long projectId) {
        Optional<AISummary> summary = summaryRepository.findByProjectId(projectId);
        return summary.map(AISummaryMapper::toDTO);
    }

    /**
     * Thu thập số liệu hiện tại của dự án và yêu cầu AI viết một bản báo cáo mới.
     */
    public AISummaryDTO generateProjectSummary(Long projectId) {
        try {
            LogUtil.info("Start crawling data to generate AI summary for project with ID: " + projectId);

            // 1. Gom nhóm toàn bộ dữ liệu thống kê của dự án
            ProjectData data = aggregateProjectData(projectId);

            // 2. Xây dựng câu lệnh (Prompt) Tiếng Việt gửi cho AI
            String prompt = buildPrompt(data);

            // 3. Gọi AI xử lý
            LogUtil.info("Waiting for AI analysing and summary generating...");
            String summaryText = aiClient.generateSummary(prompt);

            // 4. Tạo đối tượng và lưu xuống Database
            AISummary summary = new AISummary();
            summary.setProjectId(projectId);
            summary.setSummaryText(summaryText);
            summary.setGeneratedAt(LocalDateTime.now());
            summary.setPostsAnalyzed(data.totalPostsCount);
            summary.setCommentsAnalyzed(data.totalCommentsCount);
            summary.setModel(aiClient.getModelName());

            // 5. Lưu vào database
            AISummary savedSummary = summaryRepository.save(summary);

            return AISummaryMapper.toDTO(savedSummary);

        } catch (AIClientException e) {
            LogUtil.error("Error AI Client: " + e.getMessage(), e);
            throw new RuntimeException("Failed to communicate with AI: " + e.getMessage(), e);
        } catch (Exception e) {
            LogUtil.error("Unknown error while generating AI summary: " + e.getMessage(), e);
            throw new RuntimeException("System error while generating AI summary: " + e.getMessage(), e);
        }
    }

    /**
     * Đóng gói số liệu thành một câu lệnh Prompt chuẩn xác bằng Tiếng Việt.
     */
    private String buildPrompt(ProjectData data) {
        StringBuilder prompt = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        prompt.append("Bạn là một chuyên gia phân tích dữ liệu mạng xã hội và thảm họa thiên tai. ");
        prompt.append("Hãy viết một bản BÁO CÁO TỔNG HỢP TÌNH HÌNH bằng Tiếng Việt dựa trên các số liệu sau:\n\n");

        prompt.append("THÔNG TIN DỰ ÁN:\n");
        prompt.append("- Tên dự án: ").append(data.project.getName()).append("\n");
        prompt.append("- Sự kiện/Thảm họa: ").append(data.project.getDisasterName()).append("\n");
        prompt.append("- Số liệu phân tích: ").append(data.totalPostsCount).append(" bài viết và ")
                .append(data.totalCommentsCount).append(" bình luận.\n");

        if (data.project.getStartDate() != null && data.project.getEndDate() != null) {
            prompt.append("- Thời gian quét: Từ ")
                    .append(data.project.getStartDate().format(dateFormatter))
                    .append(" đến ")
                    .append(data.project.getEndDate().format(dateFormatter)).append("\n");
        }
        prompt.append("\n");

        prompt.append("THỐNG KÊ CẢM XÚC CỘNG ĐỒNG:\n");
        long totalSentiment = data.sentimentDistribution.values().stream().mapToLong(Long::longValue).sum();

        // Hiển thị dưới dạng "- <Sentiment>: <percentage>%"
        if (totalSentiment > 0) {
            for (Sentiment sentiment : Sentiment.values()) {
                long count = data.sentimentDistribution.getOrDefault(sentiment, 0L);
                double percentage = (count * 100.0) / totalSentiment;
                String sentimentVi = switch(sentiment) {
                    case POSITIVE -> "Tích cực";
                    case NEGATIVE -> "Tiêu cực";
                    case NEUTRAL -> "Trung lập";
                };
                prompt.append("- ").append(sentimentVi).append(": ")
                        .append(String.format("%.1f%%", percentage))
                        .append(" (").append(count).append(" lượt)\n");
            }
        } else {
            prompt.append("- Chưa có đủ dữ liệu cảm xúc\n");
        }
        prompt.append("\n");

        prompt.append("CÁC HẠNG MỤC THIỆT HẠI ĐƯỢC NHẮC TỚI NHIỀU NHẤT:\n");
        if (!data.damageCategoryDistribution.isEmpty()) {
            // Hiển thị dưới dạng "- <DamageCate>: <number> lượt nhắc tới"
            data.damageCategoryDistribution.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .sorted(Map.Entry.<DamageCategory, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        prompt.append("- ").append(entry.getKey().getDisplayName())
                                .append(": ").append(entry.getValue())
                                .append(" lượt nhắc tới\n");
                    });
        } else {
            prompt.append("- Chưa ghi nhận dữ liệu phân loại thiệt hại cụ thể\n");
        }
        prompt.append("\n");

        prompt.append("MỘT SỐ NỘI DUNG/BÀI VIẾT TIÊU BIỂU TRÊN MẠNG XÃ HỘI:\n");
        for (int i = 0; i < data.samplePosts.size(); i++) {
            Post post = data.samplePosts.get(i);
            String content = post.getPreprocessedContent() != null ? post.getPreprocessedContent() : post.getContent();
            if (content != null && !content.isEmpty()) {
                String truncatedContent = content.length() > 300 ? content.substring(0, 300) + "..." : content;
                prompt.append(i + 1).append(". \"").append(truncatedContent).append("\"\n");
            }
        }
        prompt.append("\n");

        prompt.append("YÊU CẦU ĐẦU RA BẮT BUỘC:\n");
        prompt.append("Bạn PHẢI trình bày kết quả chính xác bên trong các thẻ (tags) sau đây để phần mềm của tôi có thể bóc tách. " +
                      "Không được viết bất cứ chữ nào nằm ngoài các thẻ này:\n\n");
        prompt.append("<header>\n");
        prompt.append("(Viết 1 đoạn tóm tắt ngắn gọn về quy mô chiến dịch quét dữ liệu, tên thảm họa và tổng quan tình hình chung. " +
                      "Có thể liệt kê lại số lượng bài viết/bình luận đã quét).\n");
        prompt.append("</header>\n\n");

        prompt.append("<sentiment>\n");
        prompt.append("(Phân tích CHUYÊN SÂU về biểu đồ Cảm xúc cộng đồng. Tại sao có nhiều chỉ số Tiêu cực/Tích cực? Dẫn chứng từ các " +
                      "bài viết. Cuối đoạn, hãy đưa ra 1-2 đề xuất giải pháp để ổn định tâm lý người dân).\n");
        prompt.append("</sentiment>\n\n");

        prompt.append("<damage>\n");
        prompt.append("(Phân tích CHUYÊN SÂU về biểu đồ Hạng mục thiệt hại. Khu vực/Tài sản nào đang bị ảnh hưởng nặng nề nhất dựa trên số" +
                      " liệu? Cuối đoạn, đưa ra 1-2 đề xuất ưu tiên phân bổ nguồn lực cứu hộ/tái thiết).\n");
        prompt.append("</damage>\n\n");

        prompt.append("<recommendation>\n");
        prompt.append("(Đưa ra 5 hành động tóm lược có mức độ khẩn cấp giảm dần dành cho chính quyền hoặc đội cứu hộ).\n");
        prompt.append("</recommendation>\n");

        return prompt.toString();
    }

    private ProjectData aggregateProjectData(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dự án với ID: " + projectId));

        // Lấy danh sách mẫu bài viết mới nhất để làm dẫn chứng cho AI
        List<Post> allPosts = postRepository.findByProjectId(projectId);
        // Lọc ra các bài post mới nhất
        List<Post> samplePosts = allPosts.stream()
                .sorted(Comparator.comparing(Post::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_POSTS)
                .collect(Collectors.toList());

        List<Comment> allComments = commentRepository.findByProjectId(projectId);

        // Lấy dữ liệu thống kê từ Repository
        Map<Sentiment, Long> postSentiment = postRepository.countByProjectIdAndSentiment(projectId);
        Map<Sentiment, Long> commentSentiment = commentRepository.countByProjectIdAndSentiment(projectId);

        // Merge dữ liệu của Comments và Posts
        Map<Sentiment, Long> sentimentDistribution = new HashMap<>(postSentiment);
        for (Map.Entry<Sentiment, Long> entry : commentSentiment.entrySet()) {
            sentimentDistribution.merge(entry.getKey(), entry.getValue(), Long::sum);
        }

        // Dạng của Map<DamageCategory, Long>: [SẬP_NHÀ: 10, NGẬP_LỤT: 5]
        Map<DamageCategory, Long> postDamage = postRepository.countByProjectIdAndDamageCategory(projectId);
        Map<DamageCategory, Long> commentDamage = commentRepository.countByProjectIdAndDamageCategory(projectId);

        // Merge dữ liệu của Comments và Posts
        Map<DamageCategory, Long> damageDistribution = new HashMap<>(postDamage);
        for (Map.Entry<DamageCategory, Long> entry : commentDamage.entrySet()) {
            damageDistribution.merge(entry.getKey(), entry.getValue(), Long::sum);
        }

        return new ProjectData(project, allPosts.size(), allComments.size(), samplePosts, sentimentDistribution, damageDistribution);
    }

    // Lớp chứa dữ liệu nội bộ
    // Không chứa Comments tại dễ gây dư thùa dữ liệu không cần thiết
    private static class ProjectData {
        final Project project;
        final int totalPostsCount;
        final int totalCommentsCount;
        final List<Post> samplePosts;
        final Map<Sentiment, Long> sentimentDistribution;
        final Map<DamageCategory, Long> damageCategoryDistribution;

        ProjectData(Project project, int totalPostsCount, int totalCommentsCount, List<Post> samplePosts,
                    Map<Sentiment, Long> sentimentDistribution, Map<DamageCategory, Long> damageCategoryDistribution) {
            this.project = project;
            this.totalPostsCount = totalPostsCount;
            this.totalCommentsCount = totalCommentsCount;
            this.samplePosts = samplePosts;
            this.sentimentDistribution = sentimentDistribution;
            this.damageCategoryDistribution = damageCategoryDistribution;
        }
    }
}