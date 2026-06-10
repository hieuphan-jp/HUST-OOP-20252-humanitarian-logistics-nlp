package com.disaster.analysis.domain.contract.repository;

import com.disaster.analysis.domain.model.entities.Project;
import java.util.List;
import java.util.Optional;

/**
 * Giao diện (Port) định nghĩa các thao tác truy xuất dữ liệu cho Thực thể Dự án (Project).
 * Tầng Domain chỉ quan tâm "Cần làm gì", còn "Làm thế nào" (JDBC/SQL Server) sẽ do tầng Infrastructure lo.
 */
public interface ProjectRepository {

    /**
     * Lưu mới một dự án vào cơ sở dữ liệu.
     * @param project Đối tượng dự án cần lưu.
     * @return Dự án đã được cấp ID từ Database.
     */
    Project save(Project project);

    /**
     * Tìm kiếm dự án theo ID định danh.
     * @param id ID của dự án.
     * @return Optional rỗng nếu không tìm thấy, ngược lại chứa dữ liệu dự án.
     */
    Optional<Project> findById(Long id);

    /**
     * Lấy danh sách toàn bộ các dự án đang có trong hệ thống.
     * @return Danh sách Project.
     */
    List<Project> findAll();

    /**
     * Xóa một dự án khỏi hệ thống (Lưu ý: Sẽ xóa luôn Post và Comment liên quan do CASCADE).
     * @param id ID của dự án cần xóa.
     */
    void delete(Long id);

    /**
     * Cập nhật thông tin của một dự án đã tồn tại.
     * @param project Đối tượng dự án mang dữ liệu mới.
     */
    void update(Project project);
}