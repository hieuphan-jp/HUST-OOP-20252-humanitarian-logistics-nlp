package com.disaster.analysis.application.mapper;

import com.disaster.analysis.application.dto.PostDTO;
import com.disaster.analysis.domain.model.entities.Post;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Platform;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Lớp hỗ trợ chuyển đổi qua lại giữa Thực thể bài viết (Post Entity) và Đối tượng vận chuyển (PostDTO).
 * Đảm bảo cô lập dữ liệu thô dạng chuỗi của Database khỏi các cấu trúc hướng đối tượng của tầng UI.
 */
public class PostMapper {

    /**
     * Chuyển đổi từ Thực thể bài viết dưới Database thành Đối tượng DTO hiển thị lên UI.
     *
     * @param entity Đối tượng {@link Post} lấy từ Repository.
     * @return Đối tượng {@link PostDTO} đã được chuẩn hóa cấu trúc dữ liệu.
     */
    public static PostDTO toDTO(Post entity) {
        if (entity == null) {
            return null;
        }

        PostDTO dto = new PostDTO();
        dto.setId(entity.getId());
        dto.setProjectId(entity.getProjectId());
        dto.setPlatformId(entity.getPlatformId());
        dto.setContent(entity.getContent());
        dto.setAuthor(entity.getAuthor());
        dto.setPublishedAt(entity.getPublishedAt());
        dto.setUrl(entity.getUrl());
        dto.setPreprocessedContent(entity.getPreprocessedContent());
        dto.setSentiment(entity.getSentiment());
        dto.setCollectedAt(entity.getCollectedAt());

        // CHUYỂN ĐỔI: Chuỗi chữ "YOUTUBE" → Enum Platform.YOUTUBE
        if (entity.getPlatform() != null && !entity.getPlatform().isEmpty()) {
            dto.setPlatform(Platform.valueOf(entity.getPlatform().toUpperCase()));
        }

        // CHUYỂN ĐỔI: Chuỗi "BUILDING_DAMAGE,OTHER" → Set<DamageCategory>
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
     * Chuyển đổi dữ liệu từ tầng giao diện (PostDTO) thành Thực thể để lưu xuống Database.
     *
     * @param dto Đối tượng {@link PostDTO} chứa dữ liệu người dùng nhập hoặc cào về.
     * @return Thực thể {@link Post} sẵn sàng nạp vào câu lệnh SQL INSERT/UPDATE.
     */
    public static Post toEntity(PostDTO dto) {
        if (dto == null) {
            return null;
        }

        Post entity = new Post();
        entity.setId(dto.getId());
        entity.setProjectId(dto.getProjectId());
        entity.setPlatformId(dto.platformId());
        entity.setContent(dto.getContent());
        entity.setAuthor(dto.getAuthor());
        entity.setPublishedAt(dto.getPublishedAt());
        entity.setUrl(dto.getUrl());
        entity.setPreprocessedContent(dto.getPreprocessedContent());
        entity.setSentiment(dto.getSentiment());
        entity.setCollectedAt(dto.getCollectedAt());

        // CHUYỂN ĐỔI: Enum Platform → Chuỗi chữ lưu Database (VD: "YOUTUBE")
        if (dto.getPlatform() != null) {
            entity.setPlatform(dto.getPlatform().name());
        }

        // CHUYỂN ĐỔI: Set<DamageCategory> → Chuỗi chữ cách nhau bằng dấu phẩy (VD: "BUILDING_DAMAGE,OTHER")
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