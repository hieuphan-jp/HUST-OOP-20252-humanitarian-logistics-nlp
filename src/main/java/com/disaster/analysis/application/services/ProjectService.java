package com.disaster.analysis.application.services;

import com.disaster.analysis.application.dto.CommentDTO;
import com.disaster.analysis.application.dto.PostDTO;
import com.disaster.analysis.application.dto.ProjectDTO;
import com.disaster.analysis.application.mapper.CommentMapper;
import com.disaster.analysis.application.mapper.PostMapper;
import com.disaster.analysis.application.mapper.ProjectMapper;
import com.disaster.analysis.domain.model.entities.Comment;
import com.disaster.analysis.domain.model.entities.Post;
import com.disaster.analysis.domain.model.entities.Project;
import com.disaster.analysis.domain.contract.repository.CommentRepository;
import com.disaster.analysis.domain.contract.repository.PostRepository;
import com.disaster.analysis.domain.contract.repository.ProjectRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Dịch vụ quản lý vòng đời của Dự án.
 * Đóng vai trò cầu nối giữa Giao diện (UI) và Cơ sở dữ liệu (Database).
 * Chỉ nhận vào DTO và trả về DTO.
 */
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public ProjectService(ProjectRepository projectRepository, PostRepository postRepository,
                          CommentRepository commentRepository) {
        this.projectRepository = projectRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * Tạo dự án mới. Nhận thẳng DTO từ giao diện thay vì từng tham số lẻ tẻ.
     */
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        if (projectDTO == null) {
            throw new IllegalArgumentException("Dữ liệu dự án không được để trống.");
        }

        // Tự động gán thời gian khởi tạo
        projectDTO.setCreatedAt(LocalDateTime.now());
        projectDTO.setLastModified(LocalDateTime.now());

        // Thông dịch từ DTO sang Model (ép List/Enum thành String)
        Project project = ProjectMapper.toEntity(projectDTO);

        // Lưu xuống DB (SQL Server sẽ tự cấp phát ID)
        Project savedProject = projectRepository.save(project);

        // Dịch ngược từ Model sang DTO để trả về cho UI
        return ProjectMapper.toDTO(savedProject);
    }

    public List<ProjectDTO> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        return ProjectMapper.toDTOList(projects);
    }

    public Optional<ProjectDTO> getProject(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID dự án không được để trống.");
        }
        Optional<Project> project = projectRepository.findById(id);
        return project.map(ProjectMapper::toDTO);
    }

    public void deleteProject(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID dự án không được để trống.");
        }
        projectRepository.delete(id);
    }

    public void updateProject(ProjectDTO projectDTO) {
        if (projectDTO == null || projectDTO.getId() == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ hoặc thiếu ID.");
        }

        // Tự động cập nhật thời gian chỉnh sửa mới nhất
        projectDTO.setLastModified(LocalDateTime.now());

        // Ép kiểu qua Mapper và lưu xuống DB
        Project project = ProjectMapper.toEntity(projectDTO);
        projectRepository.update(project);
    }

    public List<PostDTO> getPostsByProjectId(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("ID dự án không được để trống.");
        }
        List<Post> posts = postRepository.findByProjectId(projectId);

        // Gọi Mapper để biến Model -> DTO cho UI hiển thị
        return posts.stream().map(PostMapper::toDTO).collect(Collectors.toList());
    }

    public List<CommentDTO> getCommentsByProjectId(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("ID dự án không được để trống.");
        }
        List<Comment> comments = commentRepository.findByProjectId(projectId);

        // Gọi Mapper để biến Model -> DTO cho UI hiển thị
        return comments.stream().map(CommentMapper::toDTO).collect(Collectors.toList());
    }
}