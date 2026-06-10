package com.disaster.analysis.infrastructure.repository;

import com.disaster.analysis.domain.contract.repository.SummaryRepository;
import com.disaster.analysis.domain.exception.DatabaseException;
import com.disaster.analysis.domain.model.entities.AISummary;
import com.disaster.analysis.infrastructure.repository.context.DbContext;

import java.sql.*;
import java.util.Optional;

public class SummaryRepositoryImpl implements SummaryRepository {

    @Override
    public AISummary save(AISummary summary) {
        if (summary.getProjectId() == null) throw new IllegalArgumentException("Cần có Project ID");

        // Kiểm tra xem đã có báo cáo cho dự án này chưa (Cơ chế Upsert an toàn)
        Optional<AISummary> existing = findByProjectId(summary.getProjectId());

        if (existing.isPresent()) {
            // Nếu có rồi -> Chạy UPDATE
            String updateSql = "UPDATE ai_summaries SET summary_text = ?, generated_at = ?, posts_analyzed = ?, comments_analyzed = ?, model = ? WHERE project_id = ?";
            try (Connection conn = DbContext.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, summary.getSummaryText());
                stmt.setTimestamp(2, Timestamp.valueOf(summary.getGeneratedAt()));
                stmt.setInt(3, summary.getPostsAnalyzed());
                stmt.setInt(4, summary.getCommentsAnalyzed());
                stmt.setString(5, summary.getModel());
                stmt.setLong(6, summary.getProjectId());
                stmt.executeUpdate();
                summary.setId(existing.get().getId());
                return summary;
            } catch (SQLException e) { throw new DatabaseException("Lỗi cập nhật AI Summary", e); }
        } else {
            // Nếu chưa có -> Chạy INSERT
            String insertSql = "INSERT INTO ai_summaries (project_id, summary_text, generated_at, posts_analyzed, comments_analyzed, model) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = DbContext.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, summary.getProjectId());
                stmt.setString(2, summary.getSummaryText());
                stmt.setTimestamp(3, Timestamp.valueOf(summary.getGeneratedAt()));
                stmt.setInt(4, summary.getPostsAnalyzed());
                stmt.setInt(5, summary.getCommentsAnalyzed());
                stmt.setString(6, summary.getModel());
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) summary.setId(generatedKeys.getLong(1));
                }
                return summary;
            } catch (SQLException e) { throw new DatabaseException("Lỗi lưu mới AI Summary", e); }
        }
    }

    @Override
    public Optional<AISummary> findByProjectId(Long projectId) {
        String sql = "SELECT * FROM ai_summaries WHERE project_id = ?";
        try (Connection conn = DbContext.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    AISummary summary = new AISummary();
                    summary.setId(rs.getLong("id"));
                    summary.setProjectId(rs.getLong("project_id"));
                    summary.setSummaryText(rs.getString("summary_text"));
                    summary.setGeneratedAt(rs.getTimestamp("generated_at").toLocalDateTime());
                    summary.setPostsAnalyzed(rs.getInt("posts_analyzed"));
                    summary.setCommentsAnalyzed(rs.getInt("comments_analyzed"));
                    summary.setModel(rs.getString("model"));
                    return Optional.of(summary);
                }
            }
        } catch (SQLException e) { throw new DatabaseException("Lỗi tìm kiếm Summary", e); }
        return Optional.empty();
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        String sql = "DELETE FROM ai_summaries WHERE project_id = ?";
        try (Connection conn = DbContext.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            stmt.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Lỗi xóa AI Summary", e); }
    }
}