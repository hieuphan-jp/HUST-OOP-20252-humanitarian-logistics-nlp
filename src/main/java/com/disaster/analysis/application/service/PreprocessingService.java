package com.disaster.analysis.application.service;

import com.disaster.analysis.domain.model.entities.Comment;
import com.disaster.analysis.domain.model.entities.Post;
import com.disaster.analysis.domain.contract.preprocessing.TextPreprocessor;
import com.disaster.analysis.domain.contract.repository.CommentRepository;
import com.disaster.analysis.domain.contract.repository.PostRepository;
import com.disaster.analysis.util.LogUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service đảm nhiệm việc tiền xử lý văn bản (làm sạch, chuẩn hóa) và lọc dữ liệu.
 * Đóng vai trò như một màng lọc trước khi dữ liệu được đưa vào AI phân tích.
 */
public class PreprocessingService {

    private final TextPreprocessor textPreprocessor;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public PreprocessingService(TextPreprocessor textPreprocessor,
                                PostRepository postRepository,
                                CommentRepository commentRepository) {
        this.textPreprocessor = textPreprocessor;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        LogUtil.info("PreprocessingService initialized.");
    }

    /**
     * Gọi công cụ NLP để làm sạch văn bản thô (xóa emoji, dấu câu, chuyển chữ thường).
     */
    public String preprocess(String rawContent) {
        return textPreprocessor.preprocess(rawContent);
    }

    /**
     * Kiểm tra xem nội dung bài viết có chứa bất kỳ từ khóa nào của dự án hay không.
     * @param content Nội dung cần kiểm tra.
     * @param keywords Danh sách từ khóa dự án.
     * @return true nếu có chứa từ khóa (hoặc nếu không có từ khóa nào được yêu cầu), ngược lại false.
     */
    public boolean isRelevant(String content, List<String> keywords) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        if (keywords == null || keywords.isEmpty()) {
            return true; // Nếu dự án không thiết lập từ khóa, mặc định coi tất cả là hợp lệ
        }

        String normalizedContent = content.toLowerCase();

        return keywords.stream()
                .filter(keyword -> keyword != null && !keyword.trim().isEmpty())
                .map(String::toLowerCase)
                .anyMatch(normalizedContent::contains);
    }

    /**
     * Kiểm tra xem bài viết đã tồn tại trong Cơ sở dữ liệu hay chưa (dựa vào ID của mạng xã hội).
     */
    public boolean isDuplicate(String platformId) {
        if (platformId == null || platformId.trim().isEmpty()) {
            return false;
        }
        return postRepository.existsByPlatformId(platformId);
    }

    /**
     * Cập nhật nội dung đã làm sạch vào thuộc tính preprocessedContent của đối tượng Post.
     */
    public void preprocessPost(Post post) {
        if (post == null) return;
        String preprocessedContent = preprocess(post.getContent());
        post.setPreprocessedContent(preprocessedContent);
    }

    /**
     * Xử lý hàng loạt: Lọc các bài viết không hợp lệ và tiến hành làm sạch các bài viết hợp lệ.
     * @param posts Danh sách bài viết thô.
     * @param keywords Từ khóa dùng để lọc (hiện đang được comment lại để lấy toàn bộ).
     * @return Danh sách bài viết đã được làm sạch.
     */
    public List<Post> filterAndPreprocess(List<Post> posts, List<String> keywords) {
        if (posts == null || posts.isEmpty()) {
            return List.of();
        }

        LogUtil.info("Preprocessing " + posts.size() + " posts...");

        List<Post> processedPosts = posts.stream()
                .filter(post -> post != null && post.getPlatformId() != null)
                // .filter(post -> !isDuplicate(post.getPlatformId())) // Mở comment dòng này nếu muốn chặn lưu trùng ID
                .peek(this::preprocessPost)
                // .filter(post -> isRelevant(post.getPreprocessedContent(), keywords)) // Mở comment nếu muốn lọc chặt chẽ theo từ khóa
                .collect(Collectors.toList());

        LogUtil.info("Preprocessing completed. Yielded " + processedPosts.size() + " valid posts.");
        return processedPosts;
    }

    /**
     * Kiểm tra trùng lặp cho Bình luận.
     */
    public boolean isCommentDuplicate(String platformId) {
        if (platformId == null || platformId.trim().isEmpty()) {
            return false;
        }
        return commentRepository.existsByPlatformId(platformId);
    }

    /**
     * Cập nhật nội dung đã làm sạch vào thuộc tính preprocessedContent của đối tượng Comment.
     */
    public void preprocessComment(Comment comment) {
        if (comment == null) return;
        String preprocessedContent = preprocess(comment.getContent());
        comment.setPreprocessedContent(preprocessedContent);
    }

    /**
     * Xử lý hàng loạt: Lọc và làm sạch bình luận.
     */
    public List<Comment> filterAndPreprocessComments(List<Comment> comments, List<String> keywords) {
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }

        LogUtil.info("Preprocessing " + comments.size() + " comments...");

        List<Comment> processedComments = comments.stream()
                .filter(comment -> comment != null && comment.getPlatformId() != null)
                // .filter(comment -> !isCommentDuplicate(comment.getPlatformId()))
                .peek(this::preprocessComment)
                // .filter(comment -> isRelevant(comment.getPreprocessedContent(), keywords))
                .collect(Collectors.toList());

        LogUtil.info("Preprocessing completed. Yielded " + processedComments.size() + " valid comments.");
        return processedComments;
    }
}