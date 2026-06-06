package com.disaster.analysis.domain.contract.repository;

import com.disaster.analysis.domain.model.Comment;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Sentiment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Cổng giao tiếp cho các thao tác liên quan đến Bình luận (Comment).
 */
public interface CommentRepository {

    /** Lưu mới một bình luận. */
    Comment save(Comment comment);

    /** Lưu hàng loạt bình luận cùng lúc để tối ưu I/O Database. */
    int saveBatch(List<Comment> comments);

    /** Cập nhật thông tin bình luận. */
    void update(Comment comment);

    /** Lấy toàn bộ bình luận của một Dự án (Từ tất cả các bài post trong dự án đó). */
    List<Comment> findByProjectId(Long projectId);

    /** Lấy toàn bộ bình luận nằm dưới một Bài đăng (Post) cụ thể. */
    List<Comment> findByPostId(Long postId);

    /** Lọc bình luận theo thời gian. */
    List<Comment> findByProjectIdAndDateRange(Long projectId, LocalDateTime start, LocalDateTime end);

    /** Thống kê số lượng bình luận theo từng loại Cảm xúc. */
    Map<Sentiment, Long> countByProjectIdAndSentiment(Long projectId);

    /** Thống kê số lượng bình luận theo từng Hạng mục Thiệt hại. */
    Map<DamageCategory, Long> countByProjectIdAndDamageCategory(Long projectId);

    /** Kiểm tra xem bình luận này đã tồn tại trong cơ sở dữ liệu chưa. */
    boolean existsByPlatformId(String platformId);
}