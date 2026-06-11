package com.disaster.analysis.domain.contract.repository;

/**
 * Giao diện chịu trách nhiệm tự động kiểm tra và khởi tạo cấu trúc Cơ sở dữ liệu.
 * Giúp ứng dụng có thể sử dụng được ngay khi khởi chạy mà không cần setup trước.
 */
public interface DatabaseInitializer {
    /**
     * Thực thi tiến trình kiểm tra kết nối và tự động tạo các bảng nếu chưa tồn tại.
     */
    void initialize();
}