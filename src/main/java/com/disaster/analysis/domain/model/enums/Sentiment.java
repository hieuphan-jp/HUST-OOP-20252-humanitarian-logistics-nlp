package com.disaster.analysis.domain.model.enums;

/**
 * Định nghĩa các mức độ cảm xúc của một bài viết hoặc bình luận.
 * Khi lưu xuống SQL Server, hệ thống sẽ gọi hàm .name() để lưu các chuỗi "POSITIVE", "NEGATIVE", "NEUTRAL".
 */
public enum Sentiment {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}