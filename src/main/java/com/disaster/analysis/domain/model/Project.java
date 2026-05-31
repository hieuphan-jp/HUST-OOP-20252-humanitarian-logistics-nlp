package com.disaster.analysis.domain.model;

import java.time.LocalDateTime;

/**
 * Thực thể đại diện cho một Chiến dịch/Dự án phân tích thảm họa.
 * Ánh xạ chuẩn xác 1-1 với bảng 'projects' trong cơ sở dữ liệu SQL Server.
 */
public class Project {
    private Long id;
    private String name;
    private String disasterName;
    // Lưu ý: Đổi từ List<String> sang String để khớp với cột NVARCHAR(MAX) trong SQL
    // Dữ liệu sẽ được lưu dưới dạng chuỗi cách nhau bởi dấu phẩy (VD: "bão,lũ,ngập")
    private String keywords;
    private String hashtags;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String platforms; // Đổi từ Set<Platform> sang String để lưu thẳng vào Database (VD: "FACEBOOK,YOUTUBE")

    private LocalDateTime createdAt;
    private LocalDateTime lastModified;

    /** Khởi tạo mặc định, gán sẵn thời gian tạo dự án. */
    public Project() {
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
    }

    // Getters và Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisasterName() { return disasterName; }
    public void setDisasterName(String disasterName) { this.disasterName = disasterName; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getHashtags() { return hashtags; }
    public void setHashtags(String hashtags) { this.hashtags = hashtags; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public String getPlatforms() { return platforms; }
    public void setPlatforms(String platforms) { this.platforms = platforms; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
}