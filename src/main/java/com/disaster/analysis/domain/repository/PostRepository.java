package com.disaster.analysis.domain.repository;

import com.disaster.analysis.domain.model.Post;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Sentiment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Cổng giao tiếp cho các thao tác liên quan đến Bài đăng mạng xã hội (Post).
 */
public interface PostRepository {

    /** Lưu mới một bài đăng. */
    Post save(Post post);

    /** * Lưu một danh sách nhiều bài đăng cùng lúc (Tối ưu hiệu suất so với lưu từng bài).
     * @param posts Danh sách bài đăng cần lưu.
     * @return Số lượng bài đăng đã được lưu thành công.
     */
    int saveBatch(List<Post> posts);

    /** Cập nhật thông tin bài đăng (Thường dùng sau khi AI chấm điểm xong Cảm xúc/Thiệt hại). */
    void update(Post post);

    /** Lấy toàn bộ bài đăng thuộc về một dự án cụ thể. */
    List<Post> findByProjectId(Long projectId);

    /** Lấy bài đăng của một dự án trong một khoảng thời gian nhất định. */
    List<Post> findByProjectIdAndDateRange(Long projectId, LocalDateTime start, LocalDateTime end);

    /** * Thống kê số lượng bài viết theo từng loại Cảm xúc.
     * Dùng để cung cấp số liệu cho biểu đồ và AI Summary.
     * @return Map chứa Cảm xúc làm Key, Số lượng làm Value.
     */
    Map<Sentiment, Long> countByProjectIdAndSentiment(Long projectId);

    /** * Thống kê số lượng bài viết theo từng Hạng mục Thiệt hại.
     * @return Map chứa Hạng mục làm Key, Số lượng làm Value.
     */
    Map<DamageCategory, Long> countByProjectIdAndDamageCategory(Long projectId);

    /** * Kiểm tra xem bài đăng này đã được cào về chưa (dựa trên ID của Facebook/YouTube).
     * Tránh tình trạng lưu trùng lặp dữ liệu.
     */
    boolean existsByPlatformId(String platformId);
}