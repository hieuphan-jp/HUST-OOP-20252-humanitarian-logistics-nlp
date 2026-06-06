package com.disaster.analysis.infrastructure.persistence;

import com.disaster.analysis.domain.contract.repository.PostRepository;
import com.disaster.analysis.domain.exception.DatabaseException;
import com.disaster.analysis.domain.model.Post;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Sentiment;
import com.disaster.analysis.infrastructure.persistence.context.DbContext;


import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class PostRepositoryImpl implements PostRepository {

    @Override
    public Post save(Post post) {
        String sql = "INSERT INTO posts (project_id, platform_id, platform, content, author, published_at, url, preprocessed_content, sentiment, damage_categories, collected_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setPostParameters(stmt, post);
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) post.setId(generatedKeys.getLong(1));
            }
            return post;
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi lưu bài viết: " + e.getMessage(), e);
        }
    }

    @Override
    public int saveBatch(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return 0;

        String sql = "INSERT INTO posts (project_id, platform_id, platform, content, author, published_at, url, preprocessed_content, sentiment, damage_categories, collected_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int count = 0;
            for (Post post : posts) {
                setPostParameters(stmt, post);
                stmt.addBatch();
                if (++count % 100 == 0) stmt.executeBatch();
            }
            stmt.executeBatch();
            conn.commit();
            return count;
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi lưu batch bài viết: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Post> findByProjectId(Long projectId) {
        String sql = "SELECT * FROM posts WHERE project_id = ? ORDER BY published_at DESC";
        return fetchPostList(sql, projectId, null, null);
    }

    @Override
    public List<Post> findByProjectIdAndDateRange(Long projectId, LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT * FROM posts WHERE project_id = ? AND published_at >= ? AND published_at <= ? ORDER BY published_at DESC";
        return fetchPostList(sql, projectId, start, end);
    }

    @Override
    public Map<Sentiment, Long> countByProjectIdAndSentiment(Long projectId) {
        String sql = "SELECT sentiment, COUNT(*) as count FROM posts WHERE project_id = ? AND sentiment IS NOT NULL GROUP BY sentiment";
        Map<Sentiment, Long> map = new EnumMap<>(Sentiment.class);
        for (Sentiment s : Sentiment.values()) map.put(s, 0L);

        try (Connection conn = DbContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        map.put(Sentiment.valueOf(rs.getString("sentiment")), rs.getLong("count"));
                    } catch (Exception ignored) {}
                }
            }
            return map;
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi đếm cảm xúc bài viết: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<DamageCategory, Long> countByProjectIdAndDamageCategory(Long projectId) {
        String sql = "SELECT damage_categories FROM posts WHERE project_id = ? AND damage_categories IS NOT NULL AND damage_categories != ''";
        Map<DamageCategory, Long> map = new EnumMap<>(DamageCategory.class);
        for (DamageCategory d : DamageCategory.values()) map.put(d, 0L);

        try (Connection conn = DbContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String cats = rs.getString("damage_categories");
                    if (cats != null && !cats.isEmpty()) {
                        for (String cat : cats.split(",")) {
                            try { map.merge(DamageCategory.valueOf(cat.trim()), 1L, Long::sum); } catch (Exception ignored) {}
                        }
                    }
                }
            }
            return map;
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi đếm phân loại thiệt hại: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByPlatformId(String platformId) {
        String sql = "SELECT COUNT(*) FROM posts WHERE platform_id = ?";
        try (Connection conn = DbContext.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, platformId);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
        } catch (SQLException e) { throw new DatabaseException("Lỗi kiểm tra bài viết tồn tại", e); }
    }

    @Override
    public void update(Post post) {
        String sql = "UPDATE posts SET platform = ?, content = ?, author = ?, published_at = ?, url = ?, preprocessed_content = ?, sentiment = ?, damage_categories = ? WHERE id = ?";
        try (Connection conn = DbContext.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, post.getPlatform());
            stmt.setString(2, post.getContent());
            stmt.setString(3, post.getAuthor());
            stmt.setTimestamp(4, post.getPublishedAt() != null ? Timestamp.valueOf(post.getPublishedAt()) : null);
            stmt.setString(5, post.getUrl());
            stmt.setString(6, post.getPreprocessedContent());
            stmt.setString(7, post.getSentiment() != null ? post.getSentiment().name() : null);
            stmt.setString(8, post.getDamageCategories());
            stmt.setLong(9, post.getId());
            stmt.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Lỗi cập nhật bài viết", e); }
    }

    private void setPostParameters(PreparedStatement stmt, Post post) throws SQLException {
        stmt.setLong(1, post.getProjectId());
        stmt.setString(2, post.getPlatformId());
        stmt.setString(3, post.getPlatform());
        stmt.setString(4, post.getContent());
        stmt.setString(5, post.getAuthor());
        stmt.setTimestamp(6, post.getPublishedAt() != null ? Timestamp.valueOf(post.getPublishedAt()) : null);
        stmt.setString(7, post.getUrl());
        stmt.setString(8, post.getPreprocessedContent());
        stmt.setString(9, post.getSentiment() != null ? post.getSentiment().name() : null);
        stmt.setString(10, post.getDamageCategories());
        stmt.setTimestamp(11, post.getCollectedAt() != null ? Timestamp.valueOf(post.getCollectedAt()) : Timestamp.valueOf(LocalDateTime.now()));
    }

    private List<Post> fetchPostList(String sql, Long projectId, LocalDateTime start, LocalDateTime end) {
        List<Post> list = new ArrayList<>();
        try (Connection conn = DbContext.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            if (start != null && end != null) {
                stmt.setTimestamp(2, Timestamp.valueOf(start));
                stmt.setTimestamp(3, Timestamp.valueOf(end));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Post p = new Post();
                    p.setId(rs.getLong("id"));
                    p.setProjectId(rs.getLong("project_id"));
                    p.setPlatformId(rs.getString("platform_id"));
                    p.setPlatform(rs.getString("platform"));
                    p.setContent(rs.getString("content"));
                    p.setAuthor(rs.getString("author"));
                    if (rs.getTimestamp("published_at") != null) p.setPublishedAt(rs.getTimestamp("published_at").toLocalDateTime());
                    p.setUrl(rs.getString("url"));
                    p.setPreprocessedContent(rs.getString("preprocessed_content"));
                    if (rs.getString("sentiment") != null) p.setSentiment(Sentiment.valueOf(rs.getString("sentiment")));
                    p.setDamageCategories(rs.getString("damage_categories"));
                    if (rs.getTimestamp("collected_at") != null) p.setCollectedAt(rs.getTimestamp("collected_at").toLocalDateTime());
                    list.add(p);
                }
            }
            return list;
        } catch (SQLException e) { throw new DatabaseException("Lỗi query danh sách", e); }
    }
}