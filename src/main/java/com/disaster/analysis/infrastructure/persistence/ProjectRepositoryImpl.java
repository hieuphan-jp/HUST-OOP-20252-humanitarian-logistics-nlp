package com.disaster.analysis.infrastructure.persistence;

import com.disaster.analysis.domain.contract.repository.ProjectRepository;
import com.disaster.analysis.domain.exception.DatabaseException;
import com.disaster.analysis.domain.model.Project;
import com.disaster.analysis.infrastructure.persistence.context.DbContext;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectRepositoryImpl implements ProjectRepository {

    @Override
    public Project save(Project project) {
        // Cú pháp chuẩn của SQL Server
        String sql = "INSERT INTO projects (name, disaster_name, keywords, hashtags, start_date, end_date, platforms, created_at, last_modified) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, project.getName());
            stmt.setString(2, project.getDisasterName());
            stmt.setString(3, project.getKeywords()); // Entity đã là chuỗi String, không cần ObjectMapper
            stmt.setString(4, project.getHashtags());

            if (project.getStartDate() != null) {
                stmt.setTimestamp(5, Timestamp.valueOf(project.getStartDate()));
            } else {
                stmt.setNull(5, Types.TIMESTAMP);
            }

            if (project.getEndDate() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(project.getEndDate()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }

            stmt.setString(7, project.getPlatforms());
            stmt.setTimestamp(8, Timestamp.valueOf(project.getCreatedAt() != null ? project.getCreatedAt() : LocalDateTime.now()));
            stmt.setTimestamp(9, Timestamp.valueOf(project.getLastModified() != null ? project.getLastModified() : LocalDateTime.now()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Không thể tạo dự án, không có dòng nào được lưu.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    project.setId(generatedKeys.getLong(1));
                } else {
                    throw new DatabaseException("Tạo dự án thành công nhưng không lấy được ID.");
                }
            }
            return project;

        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi lưu dự án: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Project> findById(Long id) {
        String sql = "SELECT * FROM projects WHERE id = ?";
        try (Connection conn = DbContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi tìm kiếm dự án theo ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Project> findAll() {
        String sql = "SELECT * FROM projects ORDER BY last_modified DESC";
        List<Project> projects = new ArrayList<>();
        try (Connection conn = DbContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                projects.add(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi lấy danh sách dự án: " + e.getMessage(), e);
        }
        return projects;
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM projects WHERE id = ?";
        try (Connection conn = DbContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi xóa dự án: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Project project) {
        project.setLastModified(LocalDateTime.now());
        String sql = "UPDATE projects SET name = ?, disaster_name = ?, keywords = ?, hashtags = ?, " +
                "start_date = ?, end_date = ?, platforms = ?, last_modified = ? WHERE id = ?";

        try (Connection conn = DbContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, project.getName());
            stmt.setString(2, project.getDisasterName());
            stmt.setString(3, project.getKeywords());
            stmt.setString(4, project.getHashtags());
            if (project.getStartDate() != null) stmt.setTimestamp(5, Timestamp.valueOf(project.getStartDate()));
            else stmt.setNull(5, Types.TIMESTAMP);
            if (project.getEndDate() != null) stmt.setTimestamp(6, Timestamp.valueOf(project.getEndDate()));
            else stmt.setNull(6, Types.TIMESTAMP);
            stmt.setString(7, project.getPlatforms());
            stmt.setTimestamp(8, Timestamp.valueOf(project.getLastModified()));
            stmt.setLong(9, project.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi cập nhật dự án: " + e.getMessage(), e);
        }
    }

    private Project mapResultSetToProject(ResultSet rs) throws SQLException {
        Project project = new Project();
        project.setId(rs.getLong("id"));
        project.setName(rs.getString("name"));
        project.setDisasterName(rs.getString("disaster_name"));
        project.setKeywords(rs.getString("keywords"));
        project.setHashtags(rs.getString("hashtags"));

        Timestamp startDate = rs.getTimestamp("start_date");
        if (startDate != null) project.setStartDate(startDate.toLocalDateTime());

        Timestamp endDate = rs.getTimestamp("end_date");
        if (endDate != null) project.setEndDate(endDate.toLocalDateTime());

        project.setPlatforms(rs.getString("platforms"));
        project.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        project.setLastModified(rs.getTimestamp("last_modified").toLocalDateTime());
        return project;
    }
}