package com.disaster.analysis.infrastructure.social;

import com.disaster.analysis.domain.model.Post;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class YouTubeDataSourceTest {

    private String apiKey;

    /**
     * Hàm này được đánh dấu @BeforeEach, nghĩa là nó sẽ LUÔN TỰ ĐỘNG CHẠY TRƯỚC
     * khi bài Test bên dưới được thực thi.
     * Nhiệm vụ của nó là nạp file .env để lấy API Key ra sẵn trên RAM.
     */
    @BeforeEach
    public void setUp() {
        try {
            // Đọc file .env nằm ở thư mục gốc của dự án
            Dotenv dotenv = Dotenv.load();
            apiKey = dotenv.get("YOUTUBE_API_KEY");
        } catch (Exception e) {
            // Nếu không tìm thấy file .env (ví dụ chạy trên máy chủ CI/CD), thử lấy từ biến môi trường hệ thống
            apiKey = System.getenv("YOUTUBE_API_KEY");
        }
    }

    @Test
    public void testYouTubeFetchPostsConnection() {
        // GÁC CỔNG 1: Kiểm tra xem bạn đã tạo file .env và điền Key chưa.
        // Nếu chưa, bài test sẽ dừng ngay lập tức và in ra câu cảnh báo màu đỏ bên dưới.
        assertNotNull(apiKey, "THẤT BẠI: Hệ thống chưa tìm thấy biến YOUTUBE_API_KEY. Hãy tạo file .env ở thư mục gốc!");
        assertFalse(apiKey.trim().isEmpty(), "THẤT BẠI: Biến YOUTUBE_API_KEY trong file .env đang bị để trống!");

        // 1. Chuẩn bị dữ liệu mẫu (Arrange)
        // Khởi tạo bộ cào dữ liệu YouTube với Key vừa lấy được
        YouTubeDataSource youtubeSource = new YouTubeDataSource(apiKey);

        // Tự nhập thời gian để test
        LocalDateTime startDate = LocalDateTime.parse("2024-09-07T15:30:00");
        LocalDateTime endDate = LocalDateTime.parse("2024-09-15T15:30:00");
        String testQuery = "Sơn Tùng"; // Từ khóa hot, chắc chắn có video trên YouTube

        // 2. Thực thi & Khẳng định (Act & Assert)
        assertDoesNotThrow(() -> {
            System.out.println("📡 Đang kết nối tới máy chủ YouTube API để tìm kiếm: '" + testQuery + "'...");

            // Ra lệnh cào thử tối đa 3 video
            List<Post> results = youtubeSource.fetchPosts(testQuery, startDate, endDate,3);

            // GÁC CỔNG 2: Kết quả trả về bắt buộc không được null
            assertNotNull(results, "Lỗi nghiêm trọng: Hàm fetchPosts trả về danh sách null.");

            System.out.println("Kết nối thành công! Số lượng video tìm thấy: " + results.size());

            // 3. Nếu tìm thấy video, in chi tiết ra để mắt người kiểm chứng
            if (!results.isEmpty()) {
                System.out.println("THÔNG TIN CHI TIẾT " + results.size() + " VIDEO CÀO ĐƯỢC:");

                for (int i = 0; i < results.size(); i++) {
                    Post video = results.get(i);
                    System.out.println("--------------------------------------------------");
                    System.out.println("Video #" + (i + 1) + ":");
                    System.out.println("   - Tiêu đề: " + video.getContent());
                    System.out.println("   - Kênh đăng: " + video.getAuthor());
                    System.out.println("   - Đường dẫn: " + video.getUrl());
                    System.out.println("   - Thời gian: " + video.getPublishedAt());

                    // GÁC CỔNG 3: Đảm bảo dữ liệu không bị rỗng cho TỪNG video
                    assertNotNull(video.getContent(), "Lỗi: Tiêu đề video thứ " + (i + 1) + " bị trống.");
                    assertNotNull(video.getUrl(), "Lỗi: URL video thứ " + (i + 1) + " bị trống.");
                }
                System.out.println("--------------------------------------------------");
            } else {
                System.out.println("⚠️ Kết nối mạng và API Key hoàn hảo, nhưng không tìm thấy video nào khớp với từ khóa trong khoảng thời gian đã cho.");
            }

        }, "LỖI API: Quá trình gọi API lên YouTube bị văng lỗi. Hãy kiểm tra lại xem API Key có gõ sai chính tả không, hoặc tài khoản Google Cloud có bị hết hạn ngạch (Quota) không!");
    }
}