package com.disaster.analysis.application.services;

import com.disaster.analysis.application.dto.CommentDTO;
import com.disaster.analysis.application.dto.PostDTO;
import com.disaster.analysis.application.mapper.CommentMapper;
import com.disaster.analysis.application.mapper.PostMapper;
import com.disaster.analysis.domain.model.Comment;
import com.disaster.analysis.domain.model.Post;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.contract.analysis.DamageClassifier;
import com.disaster.analysis.domain.contract.repository.CommentRepository;
import com.disaster.analysis.domain.contract.repository.PostRepository;
import com.disaster.analysis.util.LogUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service quản lý việc phân tích và gán nhãn các danh mục thiệt hại (Damage Categories).
 * Ví dụ: Sập nhà, ngập lụt, kêu gọi cứu trợ...
 */
public class DamageClassificationService {

    private final DamageClassifier damageClassifier;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public DamageClassificationService(DamageClassifier damageClassifier,
                                       PostRepository postRepository,
                                       CommentRepository commentRepository) {
        this.damageClassifier = damageClassifier;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * Khởi tạo, chuẩn bị bộ phân loại.
     */
    public void initialize() {
        damageClassifier.initialize();
        LogUtil.info("DamageClassificationService initialized.");
    }

    /**
     * Phân loại thiệt hại cho một đoạn văn bản.
     * @param text Văn bản cần phân loại.
     * @return Tập hợp (Set) các nhãn thiệt hại được tìm thấy.
     */
    public Set<DamageCategory> classifyDamage(String text) {
        return damageClassifier.classifyDamage(text);
    }

    /**
     * Quét toàn bộ bài viết trong dự án và gán nhãn thiệt hại vào DB.
     * @param projectId ID của dự án.
     * @return Số lượng bài viết đã được phân tích.
     */
    public int classifyProjectPosts(Long projectId) {
        if (!damageClassifier.isInitialized()) {
            initialize();
        }

        List<Post> posts = postRepository.findByProjectId(projectId);
        if (posts.isEmpty()) return 0;

        LogUtil.info("Starting damage classification for " + posts.size() + " posts in project " + projectId);

        int classifiedCount = 0;
        for (Post post : posts) {
            String textToClassify = post.getPreprocessedContent() != null && !post.getPreprocessedContent().isEmpty()
                    ? post.getPreprocessedContent()
                    : post.getContent();

            Set<DamageCategory> categories = classifyDamage(textToClassify);

            // Ép từ Set sang kiểu String (theo cấu trúc mới của class Post) và lưu lại
            String categoriesStr = categories.stream().map(Enum::name).collect(Collectors.joining(","));
            post.setDamageCategories(categoriesStr);

            postRepository.update(post);
            classifiedCount++;
        }

        return classifiedCount;
    }

    /**
     * Quét toàn bộ bình luận trong dự án và gán nhãn thiệt hại vào DB.
     * @param projectId ID của dự án.
     * @return Số lượng bình luận đã được phân tích.
     */
    public int classifyProjectComments(Long projectId) {
        if (!damageClassifier.isInitialized()) {
            initialize();
        }

        List<Comment> comments = commentRepository.findByProjectId(projectId);
        if (comments.isEmpty()) return 0;

        int classifiedCount = 0;
        for (Comment comment : comments) {
            String textToClassify = comment.getPreprocessedContent() != null && !comment.getPreprocessedContent().isEmpty()
                    ? comment.getPreprocessedContent()
                    : comment.getContent();

            Set<DamageCategory> categories = classifyDamage(textToClassify);
            String categoriesStr = categories.stream().map(Enum::name).collect(Collectors.joining(","));
            comment.setDamageCategories(categoriesStr);

            commentRepository.update(comment);
            classifiedCount++;
        }

        return classifiedCount;
    }

    /**
     * Gom nhóm dữ liệu để hiển thị lên Biểu đồ tròn (Pie Chart) của giao diện.
     * @param projectId ID dự án cần lấy thống kê.
     * @return Map chứa loại thiệt hại và tổng số lượt đề cập.
     */
    public Map<DamageCategory, Long> getDamageCategoryDistribution(Long projectId) {
        Map<DamageCategory, Long> postCounts = postRepository.countByProjectIdAndDamageCategory(projectId);
        Map<DamageCategory, Long> commentCounts = commentRepository.countByProjectIdAndDamageCategory(projectId);

        // Gộp kết quả của Posts và Comments lại làm 1
        Map<DamageCategory, Long> combinedCounts = new HashMap<>(postCounts);
        for (Map.Entry<DamageCategory, Long> entry : commentCounts.entrySet()) {
            combinedCounts.merge(entry.getKey(), entry.getValue(), Long::sum);
        }

        return combinedCounts;
    }

    /**
     * Phục vụ chức năng Lọc (Filter) trên giao diện. Lấy các bài viết chứa loại thiệt hại nhất định.
     */
    public List<PostDTO> getPostsByCategory(Long projectId, DamageCategory category) {
        if (category == null) return Collections.emptyList();

        List<Post> allPosts = postRepository.findByProjectId(projectId);
        return allPosts.stream()
                .filter(post -> post.getDamageCategories() != null && post.getDamageCategories().contains(category.name()))
                .map(PostMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Phục vụ chức năng Lọc (Filter) trên giao diện. Lấy các bình luận chứa loại thiệt hại nhất định.
     */
    public List<CommentDTO> getCommentsByCategory(Long projectId, DamageCategory category) {
        if (category == null) return Collections.emptyList();

        List<Comment> allComments = commentRepository.findByProjectId(projectId);
        return allComments.stream()
                .filter(comment -> comment.getDamageCategories() != null && comment.getDamageCategories().contains(category.name()))
                .map(CommentMapper::toDTO)
                .collect(Collectors.toList());
    }
}