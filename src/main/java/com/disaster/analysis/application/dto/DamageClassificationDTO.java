package com.disaster.analysis.application.dto;

import com.disaster.analysis.domain.model.enums.DamageCategory;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Lớp DTO (Data Transfer Object) lưu trữ kết quả phân tích các hạng mục thiệt hại của một bài đăng.
 * Đặt tại tầng Application để đóng gói dữ liệu từ Domain chuyển lên tầng giao diện (UI).
 */
public class DamageClassificationDTO {
    private Long postId;
    private Set<DamageCategory> categories;
    private Map<DamageCategory, Double> categoryScores;
    private LocalDateTime classifiedAt;
    private Map<DamageCategory, Long> categoryDistribution;
    private List<String> matchedKeywords;

    public DamageClassificationDTO() {
        this.categories = new HashSet<>();
        this.categoryScores = new HashMap<>();
        this.categoryDistribution = new HashMap<>();
        this.matchedKeywords = new ArrayList<>();
        this.classifiedAt = LocalDateTime.now();
    }

    public DamageClassificationDTO(Long postId, Set<DamageCategory> categories) {
        this();
        this.postId = postId;
        this.categories = categories != null ? new HashSet<>(categories) : new HashSet<>();
    }

    public DamageClassificationDTO(Map<DamageCategory, Long> categoryDistribution) {
        this();
        this.categoryDistribution = categoryDistribution != null ?
                new HashMap<>(categoryDistribution) : new HashMap<>();
    }

    // --- Getters và Setters ---
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public Set<DamageCategory> getCategories() { return categories; }
    public void setCategories(Set<DamageCategory> categories) {
        this.categories = categories != null ? new HashSet<>(categories) : new HashSet<>();
    }

    public Map<DamageCategory, Double> getCategoryScores() { return categoryScores; }
    public void setCategoryScores(Map<DamageCategory, Double> categoryScores) {
        this.categoryScores = categoryScores != null ? new HashMap<>(categoryScores) : new HashMap<>();
    }

    public LocalDateTime getClassifiedAt() { return classifiedAt; }
    public void setClassifiedAt(LocalDateTime classifiedAt) { this.classifiedAt = classifiedAt; }

    public Map<DamageCategory, Long> getCategoryDistribution() { return categoryDistribution; }
    public void setCategoryDistribution(Map<DamageCategory, Long> categoryDistribution) {
        this.categoryDistribution = categoryDistribution != null ? new HashMap<>(categoryDistribution) : new HashMap<>();
    }

    public List<String> getMatchedKeywords() { return matchedKeywords; }
    public void setMatchedKeywords(List<String> matchedKeywords) {
        this.matchedKeywords = matchedKeywords != null ? new ArrayList<>(matchedKeywords) : new ArrayList<>();
    }

    // Các hàm tiện ích
    public void addCategory(DamageCategory category, Double score) {
        this.categories.add(category);
        this.categoryScores.put(category, score);
    }

    public long getTotalCount() {
        return categoryDistribution.values().stream().mapToLong(Long::longValue).sum();
    }

    public double getPercentage(DamageCategory category) {
        long total = getTotalCount();
        if (total == 0) return 0.0;
        long count = categoryDistribution.getOrDefault(category, 0L);
        return (count * 100.0) / total;
    }

    public DamageCategory getMostCommonCategory() {
        return categoryDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}