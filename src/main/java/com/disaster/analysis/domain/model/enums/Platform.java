package com.disaster.analysis.domain.model.enums;

/**
 * Định nghĩa các nguồn/nền tảng mạng xã hội nơi hệ thống tiến hành cào dữ liệu (Data Crawling).
 */
public enum Platform {
    YOUTUBE("YouTube"),
    NEWS("News Sites");

    private final String displayName;

    /**
     * Khởi tạo nền tảng với tên định dạng chuẩn mực.
     * @param displayName Tên hiển thị trên giao diện (Ví dụ: "YouTube" thay vì "YOUTUBE").
     */
    Platform(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}