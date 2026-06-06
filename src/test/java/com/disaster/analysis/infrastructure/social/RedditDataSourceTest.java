package com.disaster.analysis.infrastructure.social;

import com.disaster.analysis.domain.model.Comment;
import com.disaster.analysis.domain.model.Post;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử tích hợp (Integration Test) dành cho RedditDataSource.
 * Kiểm tra khả năng kết nối mạng, phân tách cấu trúc JSON và bóc tách đệ quy bình luận.
 */
public class RedditDataSourceTest {

    @Test
    public void testRedditFetchPostsAndComments() {
        // 1. Chuẩn bị dữ liệu mẫu (Arrange)
        RedditDataSource redditSource = new RedditDataSource();

        // Cấu hình quét dữ liệu trong vòng 14 ngày qua
        LocalDateTime startDate = LocalDateTime.now().minusDays(14);
        LocalDateTime endDate = LocalDateTime.now();
        String testQuery = "Yagi"; // Từ khóa bão lũ thực tế, rất phổ biến trên các cộng đồng Reddit Việt Nam và quốc tế

        // 2. Thực thi & Khẳng định (Act & Assert)
        assertDoesNotThrow(() -> {
            System.out.println("📡 Đang kết nối tới Reddit Public JSON API để tìm từ khóa: '" + testQuery + "'...");

            // Ra lệnh cào thử tối đa 5 bài viết
            List<Post> posts = redditSource.fetchPosts(testQuery, startDate, endDate, 5);

            // GÁC CỔNG 1: Kết quả trả về bắt buộc không được null
            assertNotNull(posts, "Lỗi: Hàm fetchPosts trả về danh sách null.");
            System.out.println("Kết nối thành công! Tổng số bài viết Reddit cào được: " + posts.size());

            // 3. Nếu tìm thấy bài viết, tiến hành kiểm tra chi tiết dữ liệu và cào thử bình luận
            if (!posts.isEmpty()) {
                Post firstPost = posts.get(0);
                System.out.println("THÔNG TIN BÀI VIẾT ĐẦU TIÊN CÀO ĐƯỢC:");
                System.out.println("   - Tiêu đề/Nội dung: " + firstPost.getContent().substring(0, Math.min(120, firstPost.getContent().length())) + "...");
                System.out.println("   - Tác giả: " + firstPost.getAuthor());
                System.out.println("   - Link gốc: " + firstPost.getUrl());
                System.out.println("   - Ngày đăng: " + firstPost.getPublishedAt());

                // GÁC CỔNG 2: Kiểm tra các trường dữ liệu cốt lõi và ép kiểu String Platform
                assertNotNull(firstPost.getPlatformId(), "Lỗi: Platform ID không được để trống.");
                assertEquals("REDDIT", firstPost.getPlatform(), "Lỗi: Trường Platform phải là chuỗi 'REDDIT'.");

                // GIAI ĐOẠN 2: Thử nghiệm tính năng cào Bình luận (Comments) đệ quy của bài viết này
                System.out.println("📡 Đang kích hoạt tiến trình cào bình luận đệ quy cho bài đăng này...");

                // Ra lệnh lấy tối đa 5 bình luận
                List<Comment> comments = redditSource.fetchComments(firstPost, 5);

                // GÁC CỔNG 3: Danh sách bình luận không được null
                assertNotNull(comments, "Lỗi: Hàm fetchComments trả về danh sách null.");
                System.out.println("Tổng số bình luận Reddit cào được: " + comments.size());

                if (!comments.isEmpty()) {
                    Comment firstComment = comments.get(0);
                    System.out.println("THÔNG TIN BÌNH LUẬN ĐẦU TIÊN:");
                    System.out.println("   - Nội dung: " + firstComment.getContent());
                    System.out.println("   - Tác giả: " + firstComment.getAuthor());

                    // GÁC CỔNG 4: Đồng bộ kiểu dữ liệu nền tảng cho bình luận
                    assertEquals("REDDIT", firstComment.getPlatform(), "Lỗi: Trường Platform của bình luận phải là 'REDDIT'.");
                } else {
                    System.out.println("⚠️ Không tìm thấy bình luận nào cho bài viết này (Có thể đây là bài đăng rất mới).");
                }

            } else {
                System.out.println("⚠️ Kết nối mạng tốt, nhưng không tìm thấy bài đăng Reddit nào chứa từ khóa '" + testQuery + "' trong 14 ngày qua.");
            }

        }, "LỖI NGHIÊM TRỌNG: Luồng thu thập dữ liệu Reddit văng lỗi ngoại lệ! Hãy kiểm tra lại kết nối mạng.");
    }
}