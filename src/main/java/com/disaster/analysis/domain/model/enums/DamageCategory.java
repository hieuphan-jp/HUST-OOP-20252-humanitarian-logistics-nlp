package com.disaster.analysis.domain.model.enums;

/**
 * Định nghĩa các danh mục thiệt hại do thảm họa gây ra.
 * Có tích hợp tên hiển thị (displayName) để in ra giao diện UI hoặc báo cáo cho đẹp mắt.
 */
public enum DamageCategory {
    PEOPLE_AFFECTED("People Affected"),
    ECONOMIC_DISRUPTION("Economic Disruption"),
    BUILDING_DAMAGE("Building Damage"),
    PERSONAL_PROPERTY_LOSS("Personal Property Loss"),
    INFRASTRUCTURE_DAMAGE("Infrastructure Damage"),
    OTHER("Other");

    private final String displayName;

    /**
     * Khởi tạo Enum với tên hiển thị.
     * @param displayName Tên tiếng Anh (hoặc tiếng Việt) dùng để hiển thị trên UI.
     */
    DamageCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Lấy tên hiển thị của danh mục.
     * @return Tên hiển thị (Ví dụ: "Building Damage").
     */
    public String getDisplayName() {
        return displayName;
    }
}