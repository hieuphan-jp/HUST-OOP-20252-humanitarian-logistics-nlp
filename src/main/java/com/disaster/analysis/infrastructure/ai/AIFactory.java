package com.disaster.analysis.infrastructure.ai;

import com.disaster.analysis.domain.contract.ai.AIClient;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Scanner;


/**
 * Lớp Factory (Mẫu thiết kế Factory Pattern).
 * Chịu trách nhiệm đọc cấu hình và khởi tạo đúng loại AI Client (Gemini, OpenAI hoặc Mock).
 */
public class AIFactory {

    /**
     * Khởi tạo AI Client dựa trên biến ACTIVE_AI trong file .env.
     */
    public static AIClient getActiveClient() {
        Dotenv dotenv = Dotenv.load();

        // Đọc công tắc từ file .env (Mặc định dùng MOCK nếu không cấu hình để tránh lỗi)
        String activeAi = dotenv.get("ACTIVE_AI", "MOCK").toUpperCase();

        switch (activeAi) {
            case "GEMINI":
                String geminiKey = dotenv.get("GEMINI_API_KEY");
                return new GeminiAIClient(geminiKey);

            case "OPENAI":
                String openAiKey = dotenv.get("OPENAI_API_KEY");
                return new OpenAIClient(openAiKey);

            case "MOCK":
            default:
                return new MockAIClient();
        }
    }

//    /**
//     * Hàm kiểm thử độc lập (Test) luồng hoạt động của AI.
//     * Cho phép người dùng nhập văn bản, tự động đóng gói JSON gửi lên API và in kết quả trả về.
//     */
//    public static void main(String[] args) {
//        System.out.println("=== HỆ THỐNG KIỂM THỬ AI CLIENT ===");
//        AIClient client = getActiveClient();
//
//        if (client != null) {
//            System.out.println("[+] Khởi tạo thành công!");
//            System.out.println("[+] Mô hình đang sử dụng: " + client.getModelName());
//
//            // Kiểm tra xem Key đã được điền chưa
//            if (!client.isAvailable()) {
//                System.err.println("[-] LỖI: Chưa có API Key! Hãy kiểm tra lại file .env của bạn.");
//                return;
//            }
//
//            try (Scanner scanner = new Scanner(System.in)) {
//                System.out.println("\n--- BẮT ĐẦU CHAT VỚI AI (Gõ 'exit' để thoát) ---");
//
//                while (true) {
//                    System.out.print("\nNhập Prompt (Yêu cầu) của bạn: ");
//                    String prompt = scanner.nextLine();
//
//                    if ("exit".equalsIgnoreCase(prompt.trim())) {
//                        System.out.println("Đã đóng kết nối thử nghiệm.");
//                        break;
//                    }
//
//                    if (prompt.trim().isEmpty()) {
//                        continue;
//                    }
//
//                    System.out.println("\n[Hệ thống]: Đang đóng gói JSON...");
//                    System.out.println("[Hệ thống]: Đang gửi yêu cầu qua môi trường Internet...");
//
//                    // Lệnh này sẽ tự động parse sang JSON, gửi đi, nhận JSON về và bóc tách lấy chữ
//                    long startTime = System.currentTimeMillis();
//                    String response = client.generateSummary(prompt);
//                    long endTime = System.currentTimeMillis();
//
//                    System.out.println("[Hệ thống]: Phản hồi thành công sau " + (endTime - startTime) + "ms!");
//                    System.out.println("\n[AI TRẢ VỜI]:\n" + response);
//                    System.out.println("\n--------------------------------------------------");
//                }
//            } catch (Exception e) {
//                System.err.println("\n[-] Đã xảy ra lỗi trong quá trình giao tiếp với AI:");
//                e.printStackTrace();
//            }
//        } else {
//            System.err.println("[-] Kết nối thất bại: Không thể khởi tạo AI!");
//        }
//    }
}