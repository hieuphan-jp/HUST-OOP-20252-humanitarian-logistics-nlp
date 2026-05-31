package com.disaster.analysis.util;

import com.disaster.analysis.domain.model.enums.TimeGranularity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Lớp tiện ích hỗ trợ xử lý và định dạng thời gian (Ngày/Giờ).
 * Cung cấp các hàm chuẩn hóa hiển thị, tính toán khoảng cách và nhóm thời gian theo chu kỳ.
 */
public class DateTimeUtil {

    // Các bộ định dạng (Formatter) chuẩn của hệ thống
    public static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Khởi tạo private để ngăn chặn việc tạo đối tượng bằng từ khóa 'new'.
     */
    private DateTimeUtil() {}

    /**
     * Định dạng đối tượng LocalDateTime thành chuỗi dễ đọc (VD: 2024-09-07 14:30:00).
     */
    public static String formatForDisplay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DISPLAY_FORMATTER);
    }

    /**
     * Rút trích và định dạng lấy phần Ngày (VD: 2024-09-07).
     */
    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_FORMATTER);
    }

    /**
     * Rút trích và định dạng lấy phần Giờ (VD: 14:30:00).
     */
    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TIME_FORMATTER);
    }

    /**
     * Định dạng theo chuẩn ISO (Thường dùng để giao tiếp API).
     */
    public static String formatISO(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(ISO_FORMATTER);
    }

    /**
     * Chuyển đổi từ chuỗi văn bản (yyyy-MM-dd HH:mm:ss) sang đối tượng LocalDateTime.
     */
    public static LocalDateTime parseDisplay(String dateTimeString) {
        return LocalDateTime.parse(dateTimeString, DISPLAY_FORMATTER);
    }

    /**
     * Nhóm mốc thời gian theo chu kỳ (Giờ, Ngày, Tuần, Tháng) để phục vụ vẽ biểu đồ thống kê.
     *
     * @param dateTime Thời điểm cần xử lý.
     * @param granularity Chu kỳ muốn nhóm (HOURLY, DAILY, WEEKLY, MONTHLY).
     * @return Thời điểm đã được làm tròn xuống mốc chu kỳ gần nhất.
     */
    public static LocalDateTime groupByGranularity(LocalDateTime dateTime, TimeGranularity granularity) {
        if (dateTime == null) {
            return null;
        }

        return switch (granularity) {
            case HOURLY -> dateTime.truncatedTo(ChronoUnit.HOURS);
            case DAILY -> dateTime.truncatedTo(ChronoUnit.DAYS);
            case WEEKLY -> dateTime.truncatedTo(ChronoUnit.DAYS)
                    .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            case MONTHLY -> dateTime.truncatedTo(ChronoUnit.DAYS)
                    .with(TemporalAdjusters.firstDayOfMonth());
        };
    }

    /**
     * Kiểm tra xem ngày bắt đầu và ngày kết thúc có hợp lệ logic không.
     * @return true nếu ngày bắt đầu không lớn hơn ngày kết thúc.
     */
    public static boolean isValidDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !startDate.isAfter(endDate);
    }

    /**
     * Kiểm tra tính hợp lệ của khoảng thời gian và ném lỗi nếu sai logic.
     * @throws IllegalArgumentException Nếu ngày/tháng bị null hoặc thời gian bắt đầu lớn hơn kết thúc.
     */
    public static void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Ngày bắt đầu không được để trống (null)");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("Ngày kết thúc không được để trống (null)");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
    }

    /**
     * Kiểm tra xem một thời điểm cụ thể có nằm trong khoảng (Start - End) hay không.
     */
    public static boolean isWithinRange(LocalDateTime dateTime, LocalDateTime startDate, LocalDateTime endDate) {
        if (dateTime == null || startDate == null || endDate == null) {
            return false;
        }
        return !dateTime.isBefore(startDate) && !dateTime.isAfter(endDate);
    }

    /**
     * Tính tổng số giờ chênh lệch giữa hai thời điểm.
     */
    public static long getHoursBetween(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(startDate, endDate);
    }

    /**
     * Tính tổng số ngày chênh lệch giữa hai thời điểm.
     */
    public static long getDaysBetween(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
}