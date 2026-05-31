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
 * Lớp thực thi giao tiếp với API của Google Gemini.
 */
public class GeminiAIClient implements AIClient {

//    Tách riêng Base URL và Model Name theo đúng chuẩn thiết kế API
//    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
//    private static final String MODEL_NAME = "gemini-2.5-flash";
//    Tự động nối chuỗi để tạo Endpoint hoàn chỉnh
//    private static final String API_ENDPOINT = BASE_URL + MODEL_NAME + ":generateContent";

    private static final String API_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final int TIMEOUT_SECONDS = 60;

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiAIClient(String apiKey) {
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
            throw new AIClientException("Chưa cấu hình API Key cho Google Gemini");
        }

        try {
            // 1. Đóng gói dữ liệu JSON
            String requestBody = buildRequestBody(prompt);

            LogUtil.info("Đang gửi yêu cầu lên Google Gemini API...");

            // 2. Tạo HTTP Request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_ENDPOINT))
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // 3. Gửi và nhận kết quả
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                handleErrorResponse(response);
            }

            // 4. Bóc tách văn bản từ JSON trả về
            String generatedText = parseResponse(response.body());

            LogUtil.info("Gemini đã tạo báo cáo thành công.");
            return generatedText;

        } catch (IOException e) {
            throw new AIClientException("Lỗi mạng khi gọi Google Gemini API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AIClientException("Tiến trình bị gián đoạn khi gọi Google Gemini API", e);
        } catch (Exception e) {
            throw new AIClientException("Lỗi không xác định khi gọi Google Gemini API: " + e.getMessage(), e);
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
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode content = objectMapper.createObjectNode();

            ArrayNode parts = objectMapper.createArrayNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("text", prompt);
            parts.add(part);

            content.set("parts", parts);
            contents.add(content);
            root.set("contents", contents);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new AIClientException("Không thể đóng gói JSON Request: " + e.getMessage(), e);
        }
    }

    private String parseResponse(String responseBody) throws AIClientException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.get("candidates");
            if (candidates == null || !candidates.isArray() || candidates.size() == 0) {
                throw new AIClientException("Không tìm thấy kết quả (candidates) trong phản hồi của Gemini");
            }

            JsonNode firstCandidate = candidates.get(0);
            JsonNode content = firstCandidate.get("content");
            JsonNode parts = content.get("parts");
            JsonNode firstPart = parts.get(0);
            return firstPart.get("text").asText();

        } catch (Exception e) {
            throw new AIClientException("Không thể đọc JSON phản hồi từ Gemini: " + e.getMessage(), e);
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

        switch (statusCode) {
            case 400: throw new AIClientException("Yêu cầu không hợp lệ (400): " + errorMessage);
            case 401: throw new AIClientException("Lỗi xác thực (401): API Key không đúng.");
            case 403: throw new AIClientException("Bị từ chối truy cập (403): " + errorMessage);
            case 429: throw new AIClientException("Vượt quá giới hạn (429): Quá nhiều yêu cầu, vui lòng thử lại sau.");
            case 500: case 502: case 503: throw new AIClientException("Lỗi máy chủ Gemini (" + statusCode + "): " + errorMessage);
            default: throw new AIClientException(errorMessage);
        }
    }
}