package com.disaster.analysis.infrastructure.ai;

import com.disaster.analysis.domain.contract.ai.AIClient;
import io.github.cdimascio.dotenv.Dotenv;

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
}