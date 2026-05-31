package com.disaster.analysis.infrastructure.ai;

import com.disaster.analysis.domain.exception.AIClientException;
import com.disaster.analysis.domain.contract.ai.AIClient;
import com.disaster.analysis.util.LogUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Lớp thực thi giao tiếp với API của OpenAI (ChatGPT).
 */
public class OpenAIClient implements AIClient {

    private static final String API_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL_NAME = "gpt-4o-mini";
    private static final int TIMEOUT_SECONDS = 60;

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateSummary(String prompt) throws AIClientException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new AIClientException("Dữ liệu đầu vào (Prompt) không được để trống");
        }

        if (!isAvailable()) {
            throw new AIClientException("Chưa cấu hình API Key cho OpenAI");
        }

        try {
            String requestBody = buildRequestBody(prompt);
            LogUtil.info("Đang gửi yêu cầu lên OpenAI API (" + MODEL_NAME + ")...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_ENDPOINT))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                handleErrorResponse(response);
            }

            String generatedText = parseResponse(response.body());
            LogUtil.info("OpenAI đã tạo báo cáo thành công.");

            return generatedText;

        } catch (IOException e) {
            throw new AIClientException("Lỗi mạng khi gọi OpenAI API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AIClientException("Tiến trình bị gián đoạn khi gọi OpenAI API", e);
        } catch (Exception e) {
            throw new AIClientException("Lỗi không xác định khi gọi OpenAI API: " + e.getMessage(), e);
        }
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    private String buildRequestBody(String prompt) throws AIClientException {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", MODEL_NAME);

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", "user");
            message.put("content", prompt);

            messages.add(message);
            root.set("messages", messages);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new AIClientException("Không thể đóng gói JSON Request: " + e.getMessage(), e);
        }
    }

    private String parseResponse(String responseBody) throws AIClientException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                throw new AIClientException("Không tìm thấy kết quả (choices) trong phản hồi của OpenAI");
            }

            JsonNode firstChoice = choices.get(0);
            return firstChoice.get("message").get("content").asText();

        } catch (Exception e) {
            throw new AIClientException("Không thể đọc JSON phản hồi từ OpenAI: " + e.getMessage(), e);
        }
    }

    private void handleErrorResponse(HttpResponse<String> response) throws AIClientException {
        int statusCode = response.statusCode();
        String errorMessage = "Mã lỗi HTTP " + statusCode;

        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("error") && root.get("error").has("message")) {
                errorMessage = root.get("error").get("message").asText();
            }
        } catch (Exception ignored) {}

        throw new AIClientException("OpenAI Error (" + statusCode + "): " + errorMessage);
    }
}