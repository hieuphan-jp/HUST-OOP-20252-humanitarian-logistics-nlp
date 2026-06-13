package com.disaster.analysis.infrastructure.ai;

import com.disaster.analysis.domain.contract.ai.AIClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AIConnectionTest {

    @Test
    public void testAIClientResponse() {
        // 1. Chuẩn bị (Arrange)
        // Lấy client AI đang được kích hoạt từ Factory
        AIClient aiClient = AIFactory.getActiveClient();

        // Gác cổng 1: Đảm bảo Factory đã đọc được file .env và khởi tạo thành công
        assertNotNull(aiClient, "Lỗi: Hệ thống không khởi tạo được AI Client. Hãy kiểm tra lại API Key trong file .env!");

        // Câu lệnh (Prompt) cực ngắn để test mạng, tiết kiệm token tối đa
        String testPrompt = "Đây là tin nhắn kiểm tra hệ thống tự động. Hãy trả lời ngắn gọn đúng 1 chữ: 'OK'.";

        // 2. Thực thi & Khẳng định (Act & Assert)
        assertDoesNotThrow(() -> {

            // Gọi hàm sinh text của AI (Lưu ý: Thay đổi tên hàm 'generateSummary' cho khớp với interface AIClient của bạn)
            String response = aiClient.generateSummary(testPrompt);

            // Gác cổng 2: AI phải trả về chữ, không được để trống
            assertNotNull(response, "Lỗi: AI Client trả về dữ liệu null.");
            assertFalse(response.trim().isEmpty(), "Lỗi: AI Client trả về chuỗi rỗng.");

            System.out.println("Kết nối AI THÀNH CÔNG! Phản hồi từ Bot: " + response);

        }, "LỖI MẠNG HOẶC API KEY: Quá trình gọi API lên máy chủ AI bị thất bại.");
    }
}