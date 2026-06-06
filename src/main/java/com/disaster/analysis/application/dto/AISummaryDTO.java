package com.disaster.analysis.application.dto;

import java.time.LocalDateTime;

/**
 * Lớp DTO đóng gói văn bản báo cáo tóm tắt tình hình thảm họa do AI khởi tạo.
 * Được chuyển lên tầng UI hiển thị trong các vùng văn bản lớn (TextArea hoặc WebView Component).
 */
public class AISummaryDTO {
    private Long id;
    private Long projectId;
    private String summaryText;
    private LocalDateTime generatedAt;
    private int postsAnalyzed;
    private int commentsAnalyzed;
    private String model;

    public AISummaryDTO() {}

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
}