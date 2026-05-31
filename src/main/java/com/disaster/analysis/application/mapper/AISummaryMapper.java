package com.disaster.analysis.application.mapper;

import com.disaster.analysis.application.dto.AISummaryDTO;
import com.disaster.analysis.domain.model.AISummary;

/**
 * Lớp xử lý chuyển đổi dữ liệu giữa Thực thể lưu trữ báo cáo AI (AISummary) và AISummaryDTO.
 */
public class AISummaryMapper {

    /**
     * Chuyển đổi thực thể báo cáo lưu trong database thành đối tượng DTO gửi lên UI.
     */
    public static AISummaryDTO toDTO(AISummary entity) {
        if (entity == null) {
            return null;
        }

        AISummaryDTO dto = new AISummaryDTO();
        dto.setId(entity.getId());
        dto.setProjectId(entity.getProjectId());
        dto.setSummaryText(entity.getSummaryText());
        dto.setGeneratedAt(entity.getGeneratedAt());
        dto.setPostsAnalyzed(entity.getPostsAnalyzed());
        dto.setCommentsAnalyzed(entity.getCommentsAnalyzed());
        dto.setModel(entity.getModel());

        return dto;
    }

    /**
     * Chuyển đổi dữ liệu báo cáo sinh ra từ tầng Application thành thực thể lưu trữ.
     */
    public static AISummary toEntity(AISummaryDTO dto) {
        if (dto == null) {
            return null;
        }

        AISummary entity = new AISummary();
        entity.setId(dto.getId());
        entity.setProjectId(dto.getProjectId());
        entity.setSummaryText(dto.getSummaryText());
        entity.setGeneratedAt(dto.getGeneratedAt());
        entity.setPostsAnalyzed(dto.getPostsAnalyzed());
        entity.setCommentsAnalyzed(dto.getCommentsAnalyzed());
        entity.setModel(dto.getModel());

        return entity;
    }
}