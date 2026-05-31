package com.disaster.analysis.domain.exception;

import com.disaster.analysis.domain.model.enums.Platform;

/**
 * Lớp ngoại lệ (Exception) chuyên biệt dùng để đóng gói các lỗi phát sinh
 * trong quá trình cào dữ liệu từ các mạng xã hội (Ví dụ: Lỗi mạng, sai API Key, hết Quota).
 * Giúp tầng UI dễ dàng bắt lỗi và hiển thị thông báo thân thiện cho người dùng.
 */
public class DataSourceException extends DomainException {

    // Lưu trữ thông tin nền tảng nào đang gây ra lỗi (YouTube, Facebook, Reddit...)
    private final Platform platform;

    /**
     * Khởi tạo lỗi chỉ với thông báo.
     */
    public DataSourceException(Platform platform, String message) {
        super(message);
        this.platform = platform;
    }

    /**
     * Khởi tạo lỗi chứa thông báo và nguyên nhân gốc rễ (Stack Trace).
     */
    public DataSourceException(Platform platform, String message, Throwable cause) {
        super(message, cause);
        this.platform = platform;
    }

    public Platform getPlatform() {
        return platform;
    }

    @Override
    public String toString() {
        // Đã sửa lại đúng tên Class để khi Log ra file không bị nhầm lẫn
        return "DataSourceException{" +
                "platform=" + platform +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}