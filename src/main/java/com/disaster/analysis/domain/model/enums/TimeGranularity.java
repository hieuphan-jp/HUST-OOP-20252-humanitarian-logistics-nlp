package com.disaster.analysis.domain.model.enums;

/**
 * Định nghĩa các mức độ chia nhỏ thời gian (Độ phân giải).
 * Được sử dụng chủ yếu trong các hàm thống kê, nhóm dữ liệu để vẽ biểu đồ (Chart) trên giao diện.
 */
public enum TimeGranularity {
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY
}