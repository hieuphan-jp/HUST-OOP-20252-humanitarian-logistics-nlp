package com.disaster.analysis.domain.model;

import com.disaster.analysis.domain.model.enums.Sentiment;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Thực thể đại diện cho một bình luận của bài đăng mạng xã hội.
 * Ánh xạ với bảng 'comments' trong cơ sở dữ liệu.
 */
public class Comment {
    private Long id;
    private Long postId;       // Khóa ngoại liên kết tới Post
    private Long projectId;    // Khóa ngoại liên kết tới Project
    private String platformId;
    private String platform;
    private String content;
    private String author;
    private LocalDateTime publishedAt;
    private String preprocessedContent;
    private Sentiment sentiment;
    private String damageCategories;
    private LocalDateTime collectedAt;

    public Comment() {
        this.collectedAt = LocalDateTime.now();
    }

    // Getters và Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

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

    public String getPreprocessedContent() { return preprocessedContent; }
    public void setPreprocessedContent(String preprocessedContent) { this.preprocessedContent = preprocessedContent; }

    public Sentiment getSentiment() { return sentiment; }
    public void setSentiment(Sentiment sentiment) { this.sentiment = sentiment; }

    public String getDamageCategories() { return damageCategories; }
    public void setDamageCategories(String damageCategories) { this.damageCategories = damageCategories; }

    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return Objects.equals(platformId, comment.platformId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platformId);
    }
}