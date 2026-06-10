package com.disaster.analysis.application.mapper;

import com.disaster.analysis.application.dto.CommentDTO;
import com.disaster.analysis.domain.model.entities.Comment;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Platform;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Lớp hỗ trợ chuyển đổi dữ liệu giữa Thực thể bình luận (Comment Entity) và CommentDTO.
 */
public class CommentMapper {

    /**
     * Chuyển đổi Thực thể bình luận từ tầng Database sang đối tượng DTO.
     */
    public static CommentDTO toDTO(Comment entity) {
        if (entity == null) {
            return null;
        }

        CommentDTO dto = new CommentDTO();
        dto.setId(entity.getId());
        dto.setPostId(entity.getPostId());
        dto.setProjectId(entity.getProjectId());
        dto.setPlatformId(entity.getPlatformId());
        dto.setContent(entity.getContent());
        dto.setAuthor(entity.getAuthor());
        dto.setPublishedAt(entity.getPublishedAt());
        dto.setPreprocessedContent(entity.getPreprocessedContent());
        dto.setSentiment(entity.getSentiment());
        dto.setCollectedAt(entity.getCollectedAt());

        if (entity.getPlatform() != null && !entity.getPlatform().isEmpty()) {
            dto.setPlatform(Platform.valueOf(entity.getPlatform().toUpperCase()));
        }

        if (entity.getDamageCategories() != null && !entity.getDamageCategories().isEmpty()) {
            dto.setDamageCategories(
                    Arrays.stream(entity.getDamageCategories().split(","))
                            .map(String::trim)
                            .map(DamageCategory::valueOf)
                            .collect(Collectors.toSet())
            );
        } else {
            dto.setDamageCategories(new HashSet<>());
        }

        return dto;
    }

    /**
     * Chuyển đổi đối tượng dữ liệu DTO thành Thực thể bình luận để lưu trữ.
     */
    public static Comment toEntity(CommentDTO dto) {
        if (dto == null) {
            return null;
        }

        Comment entity = new Comment();
        entity.setId(dto.getId());
        entity.setPostId(dto.getPostId());
        entity.setProjectId(dto.getProjectId());
        entity.setPlatformId(dto.getPlatformId());
        entity.setContent(dto.getContent());
        entity.setAuthor(dto.getAuthor());
        entity.setPublishedAt(dto.getPublishedAt());
        entity.setPreprocessedContent(dto.getPreprocessedContent());
        entity.setSentiment(dto.getSentiment());
        entity.setCollectedAt(dto.getCollectedAt());

        if (dto.getPlatform() != null) {
            entity.setPlatform(dto.getPlatform().name());
        }

        if (dto.getDamageCategories() != null && !dto.getDamageCategories().isEmpty()) {
            entity.setDamageCategories(
                    dto.getDamageCategories().stream()
                            .map(DamageCategory::name)
                            .collect(Collectors.joining(","))
            );
        }

        return entity;
    }
}