package com.disaster.analysis.application.mapper;

import com.disaster.analysis.application.dto.ProjectDTO;
import com.disaster.analysis.domain.model.entities.Project;
import com.disaster.analysis.domain.model.enums.Platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lớp hỗ trợ chuyển đổi dữ liệu giữa Thực thể Dự án (Project Entity) và ProjectDTO.
 * Xử lý logic ép kiểu từ chuỗi văn bản (String) của Database sang các tập hợp (List, Set) của Java.
 */
public class ProjectMapper {

    /**
     * Chuyển đổi từ Thực thể Database (Entity) sang DTO để hiển thị lên giao diện UI.
     *
     * @param entity Đối tượng {@link Project} lấy từ cơ sở dữ liệu.
     * @return Đối tượng {@link ProjectDTO} chứa List và Set.
     */
    public static ProjectDTO toDTO(Project entity) {
        if (entity == null) {
            return null;
        }

        ProjectDTO dto = new ProjectDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDisasterName(entity.getDisasterName());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setLastModified(entity.getLastModified());

        // 1. CHUYỂN ĐỔI TỪ KHÓA: Chuỗi "bão,lũ,ngập" → List<String> ["bão", "lũ", "ngập"]
        if (entity.getKeywords() != null && !entity.getKeywords().trim().isEmpty()) {
            dto.setKeywords(Arrays.stream(entity.getKeywords().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList()));
        } else {
            dto.setKeywords(new ArrayList<>());
        }

        // 2. CHUYỂN ĐỔI HASHTAGS: Chuỗi "#yagi,#baoso3" → List<String> ["#yagi", "#baoso3"]
        if (entity.getHashtags() != null && !entity.getHashtags().trim().isEmpty()) {
            dto.setHashtags(Arrays.stream(entity.getHashtags().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList()));
        } else {
            dto.setHashtags(new ArrayList<>());
        }

        // 3. CHUYỂN ĐỔI NỀN TẢNG: Chuỗi "YOUTUBE,FACEBOOK" → Set<Platform>
        if (entity.getPlatforms() != null && !entity.getPlatforms().trim().isEmpty()) {
            dto.setPlatforms(Arrays.stream(entity.getPlatforms().split(","))
                    .map(String::trim)
                    .map(Platform::valueOf) // Ép chuỗi thành kiểu Enum
                    .collect(Collectors.toSet()));
        } else {
            dto.setPlatforms(new HashSet<>());
        }

        return dto;
    }

    /**
     * Chuyển đổi từ DTO của tầng giao diện thành Thực thể Entity để lưu xuống Database.
     *
     * @param dto Đối tượng {@link ProjectDTO} nhận từ thao tác người dùng.
     * @return Đối tượng {@link Project} với các mảng đã được gộp thành chuỗi.
     */
    public static Project toEntity(ProjectDTO dto) {
        if (dto == null) {
            return null;
        }

        Project entity = new Project();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDisasterName(dto.getDisasterName());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setLastModified(dto.getLastModified());

        // 1. CHUYỂN ĐỔI TỪ KHÓA: List<String> → Chuỗi "bão,lũ,ngập"
        if (dto.getKeywords() != null && !dto.getKeywords().isEmpty()) {
            entity.setKeywords(String.join(",", dto.getKeywords()));
        } else {
            entity.setKeywords("");
        }

        // 2. CHUYỂN ĐỔI HASHTAGS: List<String> → Chuỗi "#yagi,#baoso3"
        if (dto.getHashtags() != null && !dto.getHashtags().isEmpty()) {
            entity.setHashtags(String.join(",", dto.getHashtags()));
        } else {
            entity.setHashtags("");
        }

        // 3. CHUYỂN ĐỔI NỀN TẢNG: Set<Platform> → Chuỗi "YOUTUBE,FACEBOOK"
        if (dto.getPlatforms() != null && !dto.getPlatforms().isEmpty()) {
            entity.setPlatforms(dto.getPlatforms().stream()
                    .map(Platform::name)
                    .collect(Collectors.joining(",")));
        } else {
            entity.setPlatforms("");
        }

        return entity;
    }

    public static List<ProjectDTO> toDTOList(List<Project> projects) {
        if (projects == null) {
            return null;
        }

        return projects.stream()
                .map(ProjectMapper::toDTO)
                .collect(Collectors.toList());
    }
}