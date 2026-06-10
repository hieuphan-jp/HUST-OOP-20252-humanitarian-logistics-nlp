package com.disaster.analysis.application.dto;

import com.disaster.analysis.domain.model.enums.Platform;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Lớp DTO đại diện cho dữ liệu dự án chuyển tương tác với tầng UI.
 * Khác với Entity, lớp này giữ cấu trúc List và Set để JavaFX hiển thị lên các thành phần như ListView, CheckBox dễ dàng.
 */
public class ProjectDTO {
    private Long id;
    private String name;
    private String disasterName;
    private List<String> keywords;
    private List<String> hashtags;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Set<Platform> platforms;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;

    public ProjectDTO(String name, String disasterName, List<String> keywords, List<String> hashtags, LocalDateTime startDate, LocalDateTime endDate, Set<Platform> platforms){
        this.name = name;
        this.disasterName = disasterName;
        this.keywords = keywords;
        this.hashtags = hashtags;
        this.startDate = startDate;
        this.endDate = endDate;
        this.platforms = platforms;
    }

    public ProjectDTO() {}

    public ProjectDTO(String name, String disasterName, List<String> keywords, List<String> hashtags, LocalDateTime startDate, LocalDateTime endDate, Set<Platform> platforms) {
        this.name = name;
        this.disasterName = disasterName;
        this.keywords = keywords;
        this.hashtags = hashtags;
        this.startDate = startDate;
        this.endDate = endDate;
        this.platforms = platforms;
    }


    // Getters và Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisasterName() { return disasterName; }
    public void setDisasterName(String disasterName) { this.disasterName = disasterName; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<String> getHashtags() { return hashtags; }
    public void setHashtags(List<String> hashtags) { this.hashtags = hashtags; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public Set<Platform> getPlatforms() { return platforms; }
    public void setPlatforms(Set<Platform> platforms) { this.platforms = platforms; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
}