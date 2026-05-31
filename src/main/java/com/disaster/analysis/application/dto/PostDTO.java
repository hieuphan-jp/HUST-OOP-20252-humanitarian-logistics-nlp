package com.disaster.analysis.application.dto;

import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Platform;
import com.disaster.analysis.domain.model.enums.Sentiment;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Lớp DTO đóng gói thông tin của bài viết phục vụ hiển thị lên các bảng (TableView) của giao diện.
 * Chuyển đổi các chuỗi phân loại thiệt hại thô thành một tập hợp Set cấu trúc.
 */
public class PostDTO {
    private Long id;
    private Long projectId;
    private String platformId;
    private Platform platform;
    private String content;
    private String author;
    private LocalDateTime publishedAt;
    private String url;
    private String preprocessedContent;
    private Sentiment sentiment;
    private Set<DamageCategory> damageCategories;
    private LocalDateTime collectedAt;

    public PostDTO() {}

    // --- Getters và Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String platformId() { return platformId; }
    public void setPlatformId(String platformId) { this.platformId = platformId; }

    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getPreprocessedContent() { return preprocessedContent; }
    public void setPreprocessedContent(String preprocessedContent) { this.preprocessedContent = preprocessedContent; }

    public Sentiment getSentiment() { return sentiment; }
    public void setSentiment(Sentiment sentiment) { this.sentiment = sentiment; }

    public Set<DamageCategory> getDamageCategories() { return damageCategories; }
    public void setDamageCategories(Set<DamageCategory> damageCategories) { this.damageCategories = damageCategories; }

    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }
}