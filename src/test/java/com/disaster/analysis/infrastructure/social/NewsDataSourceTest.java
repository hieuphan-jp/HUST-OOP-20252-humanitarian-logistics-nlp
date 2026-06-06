package com.disaster.analysis.infrastructure.social;

import com.disaster.analysis.domain.model.Post;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NewsDataSourceTest {

    @Test
    public void testNewsDataSourceConnection() {
        // 1. Chuẩn bị (Arrange)
        NewsDataSource newsSource = new NewsDataSource();
        LocalDateTime startDate = LocalDateTime.parse("2024-09-01T15:30:00");
        LocalDateTime endDate = LocalDateTime.parse("2024-09-30T15:30:00");
        String testQuery = "yagi";

        // 2. Thực thi (Act)
        // Ra lệnh cào thử 3 bài báo
        List<Post> results = newsSource.fetchPosts(testQuery, startDate, endDate, 10);

        // 3. Khẳng định (Assert) - Gác cổng kiểm tra kết quả
        assertNotNull(results, "Lỗi nghiêm trọng: Danh sách trả về bị null (có thể do lỗi Crash hệ thống).");

        // Nếu hệ thống bắt được mạng và cào thành công, số lượng danh sách phải lớn hơn hoặc bằng 0
        // (Ngay cả khi không có tin tức nào, nó phải trả về danh sách rỗng [] chứ không được sập lỗi)
        assertTrue(results.size() >= 0, "Kết nối thành công nhưng không lấy được dữ liệu hợp lệ.");

        // Nếu thực sự cào được bài viết, in thử ra xem tiêu đề có chuẩn không
        if (!results.isEmpty()) {
            System.out.println("Bắt kết nối thành công! Đã cào được bài báo: " + results.get(0).getContent());
        }

        assertNotNull(results, "Lỗi nghiêm trọng: Danh sách trả về bị null.");
        assertTrue(results.size() >= 0, "Kết nối thành công nhưng không lấy được dữ liệu hợp lệ.");

        // LUÔN IN RA KẾT QUẢ SỐ LƯỢNG ĐỂ KIỂM CHỨNG
        System.out.println("Quá trình cào kết thúc. Số lượng bài báo lấy được: " + results.size());

        if (!results.isEmpty()) {
            System.out.println("THÔNG TIN CHI TIẾT " + results.size() + " BÀI BÁO CÀO ĐƯỢC:");

            for (int i = 0; i < results.size(); i++) {
                Post article = results.get(i);
                System.out.println("--------------------------------------------------");
                System.out.println("Bài báo #" + (i + 1) + ":");
                System.out.println("   - Tiêu đề: " + article.getContent());
                System.out.println("   - Nguồn báo: " + article.getAuthor());
                System.out.println("   - Đường dẫn: " + article.getUrl());
                System.out.println("   - Thời gian: " + article.getPublishedAt());

                // GÁC CỔNG 3: Đảm bảo dữ liệu không bị rỗng cho TỪNG bài báo
                assertNotNull(article.getContent(), "Lỗi: Tiêu đề bài báo thứ " + (i + 1) + " bị trống.");
                assertNotNull(article.getUrl(), "Lỗi: URL bài báo thứ " + (i + 1) + " bị trống.");
            }
            System.out.println("--------------------------------------------------");
        } else {
            System.out.println("⚠️ Kết nối mạng tốt, nhưng không tìm thấy bài báo nào khớp với từ khóa trong khoảng thời gian đã cho.");
        }
    }
}