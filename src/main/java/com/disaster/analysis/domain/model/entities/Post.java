package com.disaster.analysis.domain.model.entities;

import com.disaster.analysis.domain.model.enums.Sentiment;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Thực thể đại diện cho một bài đăng cào được từ mạng xã hội.
 * Ánh xạ với bảng 'posts' trong cơ sở dữ liệu.
 */
public class Post {
    private Long id;
    private Long projectId;
    private String platformId; // ID gốc của bài viết trên MXH (để tránh cào trùng lặp)
    private String platform;   // Chuyển thành String (Ví dụ: "FACEBOOK")
    private String content;
    private String author;
    private LocalDateTime publishedAt;
    private String url;
    private String preprocessedContent;
    private Sentiment sentiment; // Enum Sentiment được giữ nguyên vì JDBC có thể dễ dàng chuyển Enum thành String bằng hàm name()
    private String damageCategories; // Lưu các danh mục thiệt hại dưới dạng chuỗi (VD: "BUILDING_DAMAGE,PEOPLE_AFFECTED")
    private LocalDateTime collectedAt;

    public Post() {
        this.collectedAt = LocalDateTime.now();
    }

    // Getters và Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getPlatformId() { return platformId; }
    public void setPlatformId(String platformId) { this.platformId = platformId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

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

    public String getDamageCategories() { return damageCategories; }
    public void setDamageCategories(String damageCategories) { this.damageCategories = damageCategories; }

    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }

    /** Ghi đè hàm equals để so sánh 2 bài post có trùng ID mạng xã hội không */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return Objects.equals(platformId, post.platformId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platformId);
    }
}