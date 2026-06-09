package com.disaster.analysis.infrastructure.repository;

import com.disaster.analysis.domain.contract.repository.CommentRepository;
import com.disaster.analysis.domain.exception.DatabaseException;
import com.disaster.analysis.domain.model.entities.Comment;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Sentiment;
import com.disaster.analysis.infrastructure.repository.context.DbContext;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class CommentRepositoryImpl implements CommentRepository {

    @Override
    public Comment save(Comment comment) {
        String sql = "INSERT INTO comments (post_id, project_id, platform_id, platform, content, author, published_at, preprocessed_content, sentiment, damage_categories, collected_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setCommentParameters(stmt, comment);
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) comment.setId(generatedKeys.getLong(1));
            }
            return comment;
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi lưu bình luận: " + e.getMessage(), e);
        }
    }

    @Override
    public int saveBatch(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) return 0;
        String sql = "INSERT INTO comments (post_id, project_id, platform_id, platform, content, author, published_at, preprocessed_content, sentiment, damage_categories, collected_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbContext.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int count = 0;
            for (Comment comment : comments) {
                setCommentParameters(stmt, comment);
                stmt.addBatch();
                if (++count % 100 == 0) stmt.executeBatch();
            }
            stmt.executeBatch();
            conn.commit();
            return count;
        } catch (SQLException e) { throw new DatabaseException("Lỗi lưu batch bình luận", e); }
    }

    @Override
    public List<Comment> findByProjectId(Long projectId) {
        return fetchCommentList("SELECT * FROM comments WHERE project_id = ? ORDER BY published_at DESC", projectId);
    }

    @Override
    public List<Comment> findByPostId(Long postId) {
        return fetchCommentList("SELECT * FROM comments WHERE post_id = ? ORDER BY published_at DESC", postId);
    }

    @Override
    public List<Comment> findByProjectIdAndDateRange(Long projectId, LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT * FROM comments WHERE project_id = ? AND published_at >= ? AND published_at <= ? ORDER BY published_at DESC";
        List<Comment> list = new ArrayList<>();
        try (Connection conn = DbContext.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            stmt.setTimestamp(2, Timestamp.valueOf(start));
            stmt.setTimestamp(3, Timestamp.valueOf(end));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToComment(rs));
            }
            return list;
        } catch (SQLException e) { throw new DatabaseException("Lỗi query thời gian bình luận", e); }
    }

    @Override
    public Map<Sentiment, Long> countByProjectIdAndSentiment(Long projectId) {
        String sql = "SELECT sentiment, COUNT(*) as count FROM comments WHERE project_id = ? AND sentiment IS NOT NULL GROUP BY sentiment";
        Map<Sentiment, Long> map = new EnumMap<>(Sentiment.class);
        for (Sentiment s : Sentiment.values()) map.put(s, 0L);
        try (Connection conn = DbContext.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try { map.put(Sentiment.valueOf(rs.getString("sentiment")), rs.getLong("count")); } catch (Exception ignored) {}
                }
            }
            return map;
        } catch (SQLException e) { throw new DatabaseException("Lỗi đếm cảm xúc", e); }
    }

    @Override
    public Map<DamageCategory, Long> countByProjectIdAndDamageCategory(Long projectId) {
        String sql = "SELECT damage_categories FROM comments WHERE project_id = ? AND damage_categories IS NOT NULL AND damage_categories != ''";
        Map<DamageCategory, Long> map = new EnumMap<>(DamageCategory.class);
        for (DamageCategory d : DamageCategory.values()) map.put(d, 0L);
        try (Connection conn = DbContext.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        } catch (SQLException e) { throw new DatabaseException("Lỗi đếm phân loại", e); }
    }

    @Override
    public boolean existsByPlatformId(String platformId) {
        String sql = "SELECT COUNT(*) FROM comments WHERE platform_id = ?";
        try (Connection conn = DbContext.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, platformId);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
        } catch (SQLException e) { throw new DatabaseException("Lỗi kiểm tra", e); }
    }

    @Override
    public void update(Comment comment) {
        String sql = "UPDATE comments SET platform = ?, content = ?, author = ?, published_at = ?, preprocessed_content = ?, sentiment = ?, damage_categories = ? WHERE id = ?";
        try (Connection conn = DbContext.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, comment.getPlatform());
            stmt.setString(2, comment.getContent());
            stmt.setString(3, comment.getAuthor());
            stmt.setTimestamp(4, comment.getPublishedAt() != null ? Timestamp.valueOf(comment.getPublishedAt()) : null);
            stmt.setString(5, comment.getPreprocessedContent());
            stmt.setString(6, comment.getSentiment() != null ? comment.getSentiment().name() : null);
            stmt.setString(7, comment.getDamageCategories());
            stmt.setLong(8, comment.getId());
            stmt.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Lỗi update bình luận", e); }
    }

    private void setCommentParameters(PreparedStatement stmt, Comment comment) throws SQLException {
        stmt.setLong(1, comment.getPostId());
        stmt.setLong(2, comment.getProjectId());
        stmt.setString(3, comment.getPlatformId());
        stmt.setString(4, comment.getPlatform());
        stmt.setString(5, comment.getContent());
        stmt.setString(6, comment.getAuthor());
        stmt.setTimestamp(7, comment.getPublishedAt() != null ? Timestamp.valueOf(comment.getPublishedAt()) : null);
        stmt.setString(8, comment.getPreprocessedContent());
        stmt.setString(9, comment.getSentiment() != null ? comment.getSentiment().name() : null);
        stmt.setString(10, comment.getDamageCategories());
        stmt.setTimestamp(11, comment.getCollectedAt() != null ? Timestamp.valueOf(comment.getCollectedAt()) : Timestamp.valueOf(LocalDateTime.now()));
    }

    private List<Comment> fetchCommentList(String sql, Long idParam) {
        List<Comment> list = new ArrayList<>();
        try (Connection conn = DbContext.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idParam);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToComment(rs));
            }
            return list;
        } catch (SQLException e) { throw new DatabaseException("Lỗi truy vấn", e); }
    }

    private Comment mapResultSetToComment(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setId(rs.getLong("id"));
        c.setPostId(rs.getLong("post_id"));
        c.setProjectId(rs.getLong("project_id"));
        c.setPlatformId(rs.getString("platform_id"));
        c.setPlatform(rs.getString("platform"));
        c.setContent(rs.getString("content"));
        c.setAuthor(rs.getString("author"));
        if (rs.getTimestamp("published_at") != null) c.setPublishedAt(rs.getTimestamp("published_at").toLocalDateTime());
        c.setPreprocessedContent(rs.getString("preprocessed_content"));
        if (rs.getString("sentiment") != null) c.setSentiment(Sentiment.valueOf(rs.getString("sentiment")));
        c.setDamageCategories(rs.getString("damage_categories"));
        if (rs.getTimestamp("collected_at") != null) c.setCollectedAt(rs.getTimestamp("collected_at").toLocalDateTime());
        return c;
    }
}