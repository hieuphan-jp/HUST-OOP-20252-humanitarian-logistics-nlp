package com.disaster.analysis.domain.model.entities;

import java.time.LocalDateTime;

/**
 * Thực thể lưu trữ kết quả phân tích tổng hợp trả về từ AI.
 * Ánh xạ với bảng 'ai_summaries' trong cơ sở dữ liệu.
 */
public class AISummary {
    private Long id;
    private Long projectId;
    private String summaryText;
    private LocalDateTime generatedAt;
    private int postsAnalyzed;
    private int commentsAnalyzed;
    private String model;

    public AISummary() {
        this.generatedAt = LocalDateTime.now();
    }

    public AISummary(Long projectId, String summaryText, int postsAnalyzed, int commentsAnalyzed, String model) {
        this();
        this.projectId = projectId;
        this.summaryText = summaryText;
        this.postsAnalyzed = postsAnalyzed;
        this.commentsAnalyzed = commentsAnalyzed;
        this.model = model;
    }

    // Getters và Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public int getPostsAnalyzed() { return postsAnalyzed; }
    public void setPostsAnalyzed(int postsAnalyzed) { this.postsAnalyzed = postsAnalyzed; }

    public int getCommentsAnalyzed() { return commentsAnalyzed; }
    public void setCommentsAnalyzed(int commentsAnalyzed) { this.commentsAnalyzed = commentsAnalyzed; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    @Override
    public String toString() {
        return "AISummary{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", summaryText='" + (summaryText != null && summaryText.length() > 50
                ? summaryText.substring(0, 50) + "..."
                : summaryText) + '\'' +
                ", generatedAt=" + generatedAt +
                ", postsAnalyzed=" + postsAnalyzed +
                ", commentsAnalyzed=" + commentsAnalyzed +
                ", model='" + model + '\'' +
                '}';
    }
}