package com.disaster.analysis.domain.contract.social;

import com.disaster.analysis.domain.exception.DataSourceException;
import com.disaster.analysis.domain.model.entities.Comment;
import com.disaster.analysis.domain.model.entities.Post;
import com.disaster.analysis.domain.model.enums.Platform;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bản hợp đồng định nghĩa các "Đầu dò" (Crawler/API Client) dùng để cào dữ liệu từ mạng xã hội.
 * Các lớp như YouTubeDataSource, RedditDataSource sẽ thực thi hợp đồng này.
 */
public interface DataSource {

    /**
     * Tìm kiếm và tải về các bài viết/video dựa trên từ khóa.
     *
     * @param query      Từ khóa tìm kiếm (Ví dụ: "Bão Yagi", "Lũ lụt miền Trung").
     * @param startDate  Lọc các bài đăng từ thời điểm này.
     * @param endDate    Lọc các bài đăng đến thời điểm này.
     * @param maxResults Giới hạn số lượng bài tối đa muốn cào về.
     * @return Danh sách các bài đăng (Post) thô vừa lấy được.
     * @throws DataSourceException Nếu mạng xã hội từ chối kết nối, chặn IP, hoặc API lỗi.
     */
    List<Post> fetchPosts(String query, LocalDateTime startDate,
                          LocalDateTime endDate, int maxResults) throws DataSourceException;

    /**
     * Cào toàn bộ bình luận nằm bên dưới một bài đăng cụ thể.
     *
     * @param post       Bài đăng gốc cần lấy bình luận.
     * @param maxResults Số lượng bình luận tối đa.
     * @return Danh sách bình luận thô.
     * @throws DataSourceException Nếu không thể lấy dữ liệu.
     */
    List<Comment> fetchComments(Post post, int maxResults) throws DataSourceException;

    /**
     * Xác định xem Nguồn cào dữ liệu này thuộc về nền tảng nào.
     * @return Nền tảng (Ví dụ: Platform.YOUTUBE).
     */
    Platform getPlatform();
}