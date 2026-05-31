package com.disaster.analysis.infrastructure.analysis;

import com.disaster.analysis.domain.model.enums.Sentiment;
import com.disaster.analysis.domain.contract.analysis.SentimentAnalyzer;

/**
 * Bộ phân tích lai (Hybrid).
 * Tự động nhận diện ngôn ngữ của bài viết để gọi công cụ xử lý tương ứng (Việt/Anh).
 */
public class HybridSentimentAnalyzer implements SentimentAnalyzer {

    private final VietnameseSentimentAnalyzer vietnameseAnalyzer;
    private final StanfordSentimentAnalyzer englishAnalyzer;
    private boolean initialized = false;

    public HybridSentimentAnalyzer() {
        this.vietnameseAnalyzer = new VietnameseSentimentAnalyzer();
        this.englishAnalyzer = new StanfordSentimentAnalyzer();
    }

    @Override
    public void initialize() {
        if (!initialized) {
            vietnameseAnalyzer.initialize();
            englishAnalyzer.initialize();
            initialized = true;
        }
    }

    @Override
    public Sentiment analyzeSentiment(String text) {
        if (!initialized) {
            throw new IllegalStateException("HybridSentimentAnalyzer chưa được khởi tạo.");
        }

        if (text == null || text.trim().isEmpty()) {
            return Sentiment.NEUTRAL;
        }

        String normalizedText = text.trim().toLowerCase();
        if (isVietnamese(normalizedText)) {
            return vietnameseAnalyzer.analyzeSentiment(text);
        } else {
            return englishAnalyzer.analyzeSentiment(text);
        }
    }

    /**
     * Thuật toán nhận diện Tiếng Việt nhanh dựa trên dấu câu đặc trưng.
     */
    private boolean isVietnamese(String text) {
        String normalizedText = text.toLowerCase();
        for (char c : normalizedText.toCharArray()) {
            if (isVietnameseChar(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean isVietnameseChar(char c) {
        if (c >= '\u0100' && c <= '\u017F') return true;
        if (c >= '\u0180' && c <= '\u024F') return true;
        if (c >= '\u1E00' && c <= '\u1EFF') return true;

        return "àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ".indexOf(c) >= 0 ||
                "ÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ".indexOf(c) >= 0;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }
}