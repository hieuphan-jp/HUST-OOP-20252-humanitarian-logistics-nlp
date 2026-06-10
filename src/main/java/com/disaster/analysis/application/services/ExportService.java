package com.disaster.analysis.application.services;

import com.disaster.analysis.domain.model.entities.Comment;
import com.disaster.analysis.domain.model.entities.Post;
import com.disaster.analysis.domain.model.entities.Project;
import com.disaster.analysis.domain.contract.repository.CommentRepository;
import com.disaster.analysis.domain.contract.repository.PostRepository;
import com.disaster.analysis.domain.contract.export.Exporter;
import com.disaster.analysis.util.LogUtil;
import com.disaster.analysis.domain.exception.ExportException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Service quản lý tác vụ Kết xuất dữ liệu (Export).
 * Chịu trách nhiệm gom nhóm toàn bộ bài viết, bình luận của một Dự án
 * và chuyển giao cho công cụ Exporter để tạo ra file báo cáo (Ví dụ: Excel, CSV).
 */
public class ExportService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final Exporter exporter; // Interface trừu tượng giúp dễ dàng thay đổi thư viện xuất file (Ví dụ chuyển từ Apache POI sang fastexcel)

    /**
     * Hàm khởi tạo Service Xuất dữ liệu.
     * Cần được bơm (inject) đầy đủ các kho lưu trữ và công cụ xử lý file.
     */
    public ExportService(PostRepository postRepository, CommentRepository commentRepository, Exporter exporter) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.exporter = exporter;
        LogUtil.info("ExportService initialized successfully.");
    }

    /**
     * Thực thi lệnh kết xuất toàn bộ dữ liệu của một Dự án ra file lưu trên ổ cứng.
     * @param project Đối tượng dự án cần xuất dữ liệu.
     * @param outputPath Đường dẫn tuyệt đối lưu file (Ví dụ: C:/Downloads/BaoCaoYagi.xlsx).
     */
    public void exportProject(Project project, Path outputPath) {
        boolean exportSuccessful = false;

        try {
            // 1. Gác cổng kiểm tra tính hợp lệ của Dự án và Đường dẫn lưu file
            validateProject(project);
            validatePath(outputPath);

            LogUtil.info("Starting data export pipeline for project: " + project.getName());

            // 2. Kéo toàn bộ dữ liệu Bài viết và Bình luận từ Cơ sở dữ liệu lên RAM
            LogUtil.info("Fetching posts for project ID: " + project.getId());
            List<Post> posts = postRepository.findByProjectId(project.getId());

            LogUtil.info("Fetching comments for project ID: " + project.getId());
            List<Comment> comments = commentRepository.findByProjectId(project.getId());

            LogUtil.info("Retrieved " + posts.size() + " posts and " + comments.size() + " comments for export.");

            // 3. Ủy quyền cho công cụ Exporter (Ví dụ: XlsxExporter) tiến hành vẽ bảng và ghi file
            LogUtil.info("Creating output workbook at destination: " + outputPath.toString());
            exporter.export(project, posts, comments, outputPath);

            LogUtil.info("Export completed successfully at: " + outputPath.toString());
            exportSuccessful = true;

        } catch (ExportException e) {
            LogUtil.error("Exporter engine failed: " + e.getMessage());
            throw new RuntimeException("Lỗi trong quá trình tạo file báo cáo: " + e.getMessage(), e);
        } catch (Exception e) {
            LogUtil.error("Unexpected system error during export process", e);
            throw new RuntimeException("Lỗi hệ thống không xác định khi xuất dữ liệu: " + e.getMessage(), e);
        } finally {
            // 4. Cơ chế dọn dẹp (Rollback): Nếu quá trình ghi file bị lỗi giữa chừng (do sập nguồn, hết dung lượng...)
            // Hệ thống sẽ tự động xóa file rác viết dở dang để không làm tốn ổ cứng của người dùng
            if (!exportSuccessful) {
                cleanupPartialFile(outputPath);
            }
        }
    }

    /**
     * Hàm tiện ích kiểm tra điều kiện cần của một Dự án trước khi xuất file.
     */
    private void validateProject(Project project) {
        if (project == null) {
            throw new RuntimeException("Dữ liệu dự án truyền vào bị trống (null).");
        }

        if (project.getId() == null) {
            throw new RuntimeException("Dự án chưa được lưu vào hệ thống (Thiếu ID).");
        }

        // Cảnh báo người dùng nếu họ bấm Xuất file nhưng dự án chưa cào được chữ nào
        List<Post> posts = postRepository.findByProjectId(project.getId());
        List<Comment> comments = commentRepository.findByProjectId(project.getId());

        if (posts.isEmpty() && comments.isEmpty()) {
            throw new RuntimeException("Dự án không có dữ liệu để xuất. Vui lòng tiến hành Thu thập dữ liệu trước.");
        }
    }

    /**
     * Hàm tiện ích kiểm tra điều kiện an toàn của ổ đĩa và thư mục lưu file.
     */
    private void validatePath(Path outputPath) {
        if (outputPath == null) {
            throw new RuntimeException("Đường dẫn xuất file không được để trống.");
        }

        Path parentDir = outputPath.getParent();

        // Kiểm tra xem thư mục chứa file có tồn tại không
        if (parentDir != null && !Files.exists(parentDir)) {
            throw new RuntimeException("Thư mục lưu trữ không tồn tại: " + parentDir.toString());
        }

        // Kiểm tra quyền ghi đè (Tránh lỗi do tài khoản Windows/Mac bị giới hạn quyền)
        if (parentDir != null && !Files.isWritable(parentDir)) {
            throw new RuntimeException("Không có quyền ghi dữ liệu vào thư mục (Permission Denied): " + parentDir.toString());
        }

        // Nếu tên file đã tồn tại trên máy tính, kiểm tra xem nó có đang bị khóa bởi ứng dụng khác không (Ví dụ người dùng đang mở file Excel đó)
        if (Files.exists(outputPath) && !Files.isWritable(outputPath)) {
            throw new RuntimeException("Không thể ghi đè file. Vui lòng kiểm tra xem bạn có đang mở file này trong Excel không: " + outputPath.toString());
        }
    }

    /**
     * Cơ chế "thợ lau dọn": Tự động xóa bỏ các file tạm, file lỗi, file sinh ra dang dở.
     */
    private void cleanupPartialFile(Path outputPath) {
        if (outputPath != null && Files.exists(outputPath)) {
            try {
                Files.deleteIfExists(outputPath);
                LogUtil.info("Successfully cleaned up corrupted/partial export file: " + outputPath.toString());
            } catch (IOException e) {
                LogUtil.warn("Failed to clean up partial export file. It might be locked by OS: " + outputPath.toString());
            }
        }
    }
}