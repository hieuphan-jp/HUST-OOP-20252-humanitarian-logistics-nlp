package com.disaster.analysis.domain.contract.export;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.disaster.analysis.domain.model.entities.Comment;
import com.disaster.analysis.domain.model.entities.Post;
import com.disaster.analysis.domain.model.entities.Project;

/**
 * Bản hợp đồng định nghĩa công cụ xuất dữ liệu của hệ thống ra các file bên ngoài (PDF, Excel, CSV...).
 */
public interface Exporter {

    /**
     * Đóng gói toàn bộ dữ liệu của một chiến dịch và xuất ra file.
     *
     * @param project    Thông tin tổng quan của dự án.
     * @param posts      Danh sách bài viết đã thu thập.
     * @param comments   Danh sách bình luận liên quan.
     * @param outputPath Đường dẫn nơi file sẽ được lưu (Ví dụ: "C:/reports/baocao.xlsx").
     * @throws IOException Nếu có lỗi trong quá trình ghi file (như ổ cứng đầy, file đang bị khóa).
     */
    void export(Project project, List<Post> posts, List<Comment> comments, Path outputPath) throws IOException;
}