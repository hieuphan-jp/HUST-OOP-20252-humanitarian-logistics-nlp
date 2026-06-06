package com.disaster.analysis.application.dto;

import com.disaster.analysis.domain.model.enums.Sentiment;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Lớp DTO lưu trữ kết quả phân tích cảm xúc (Sentiment).
 * Dùng để vận chuyển số liệu thống kê lên tầng giao diện (UI) để vẽ biểu đồ mà không làm lộ Entity gốc.
 */
public class SentimentResultDTO {
    private Long postId;
    private Sentiment sentiment;
    private Double confidenceScore;
    private LocalDateTime analyzedAt;
    private Map<Sentiment, Long> sentimentCounts;
    private Map<LocalDateTime, Map<Sentiment, Long>> timeSeries;

    public SentimentResultDTO() {
        this.sentimentCounts = new HashMap<>();
        this.timeSeries = new HashMap<>();
        this.analyzedAt = LocalDateTime.now();
    }

    public SentimentResultDTO(Long postId, Sentiment sentiment, Double confidenceScore) {
        this();
        this.postId = postId;
        this.sentiment = sentiment;
        this.confidenceScore = confidenceScore;
    }

    public static SentimentResultDTO fromCounts(Map<Sentiment, Long> sentimentCounts) {
        SentimentResultDTO result = new SentimentResultDTO();
        result.sentimentCounts = sentimentCounts != null ? new HashMap<>(sentimentCounts) : new HashMap<>();
        return result;
    }

    public static SentimentResultDTO fromTimeSeries(Map<LocalDateTime, Map<Sentiment, Long>> timeSeries) {
        SentimentResultDTO result = new SentimentResultDTO();
        result.timeSeries = timeSeries != null ? new HashMap<>(timeSeries) : new HashMap<>();
        return result;
    }

    // Getters và Setters
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public Sentiment getSentiment() { return sentiment; }
    public void setSentiment(Sentiment sentiment) { this.sentiment = sentiment; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    public Map<Sentiment, Long> getSentimentCounts() { return sentimentCounts; }
    public void setSentimentCounts(Map<Sentiment, Long> sentimentCounts) {
        this.sentimentCounts = sentimentCounts != null ? new HashMap<>(sentimentCounts) : new HashMap<>();
    }

    public Map<LocalDateTime, Map<Sentiment, Long>> getTimeSeries() { return timeSeries; }
    public void setTimeSeries(Map<LocalDateTime, Map<Sentiment, Long>> timeSeries) {
        this.timeSeries = timeSeries != null ? new HashMap<>(timeSeries) : new HashMap<>();
    }

    // Các hàm tiện ích
    public long getTotalCount() {
        return sentimentCounts.values().stream().mapToLong(Long::longValue).sum();
    }

    public double getPercentage(Sentiment sentiment) {
        long total = getTotalCount();
        if (total == 0) return 0.0;
        long count = sentimentCounts.getOrDefault(sentiment, 0L);
        return (count * 100.0) / total;
    }

    @Override
    public String toString() {
        return "SentimentResult{" +
                "postId=" + postId +
                ", sentiment=" + sentiment +
                ", confidenceScore=" + confidenceScore +
                ", analyzedAt=" + analyzedAt +
                ", sentimentCounts=" + sentimentCounts +
                ", totalCount=" + getTotalCount() +
                '}';
    }
}