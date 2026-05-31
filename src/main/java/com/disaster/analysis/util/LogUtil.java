package com.disaster.analysis.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lớp tiện ích ghi nhật ký hệ thống (Logging).
 * Hỗ trợ ghi lại lịch sử hoạt động và cảnh báo lỗi ra file vật lý và Console.
 * Tự động tạo thư mục 'logs' và file nếu chưa tồn tại.
 */
public class LogUtil {

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "disaster_analysis.log";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static Path logFilePath;
    private static boolean initialized = false;

    /**
     * Định nghĩa các cấp độ cảnh báo của hệ thống.
     */
    public enum LogLevel {
        INFO,   // Thông tin hoạt động bình thường
        WARN,   // Cảnh báo (Có thể dẫn tới lỗi)
        ERROR,  // Lỗi hệ thống nghiêm trọng
        DEBUG   // Gỡ lỗi dành cho lập trình viên
    }

    private LogUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Khởi tạo file log và thư mục lưu trữ (Chỉ chạy 1 lần duy nhất).
     */
    private static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            logFilePath = logDir.resolve(LOG_FILE);
            if (!Files.exists(logFilePath)) {
                Files.createFile(logFilePath);
            }

            initialized = true;
            log(LogLevel.INFO, "Hệ thống ghi nhật ký (Logging System) đã được khởi động.");
        } catch (IOException e) {
            System.err.println("Không thể khởi tạo hệ thống Log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Hàm lõi xử lý ghi chuỗi nội dung vào file và in ra Console.
     */
    private static void log(LogLevel level, String message) {
        if (!initialized) {
            initialize();
        }

        if (logFilePath == null) {
            System.err.println("Log file chưa sẵn sàng. Nội dung bị bỏ lỡ: " + message);
            return;
        }

        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String logEntry = String.format("[%s] [%s] %s%n", timestamp, level, message);

            // Ghi nối tiếp (APPEND) vào file log
            Files.writeString(logFilePath, logEntry,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

            // In thẳng ra màn hình Console của IDE
            System.out.print(logEntry);
        } catch (IOException e) {
            System.err.println("Ghi Log thất bại: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Hàm lõi ghi log kèm theo chi tiết cấu trúc lỗi (Stack Trace) của Java.
     */
    private static void log(LogLevel level, String message, Throwable throwable) {
        if (!initialized) {
            initialize();
        }

        StringBuilder sb = new StringBuilder(message);
        sb.append(" - Exception: ").append(throwable.getClass().getName());
        sb.append(": ").append(throwable.getMessage());

        // Trích xuất dấu vết chuỗi lỗi (Stack Trace)
        sb.append("\nStack trace:\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
        }

        // Tìm và in ra nguyên nhân gốc rễ (Cause) nếu có
        Throwable cause = throwable.getCause();
        if (cause != null) {
            sb.append("Caused by: ").append(cause.getClass().getName());
            sb.append(": ").append(cause.getMessage()).append("\n");
            for (StackTraceElement element : cause.getStackTrace()) {
                sb.append("  at ").append(element.toString()).append("\n");
            }
        }

        log(level, sb.toString());
    }

    /** Ghi thông tin tiến trình bình thường */
    public static void info(String message) {
        log(LogLevel.INFO, message);
    }

    /** Ghi cảnh báo rủi ro */
    public static void warn(String message) {
        log(LogLevel.WARN, message);
    }

    /** Ghi cảnh báo rủi ro kèm thông tin Lỗi (Exception) */
    public static void warn(String message, Throwable throwable) {
        log(LogLevel.WARN, message, throwable);
    }

    /** Ghi lỗi hệ thống nghiêm trọng */
    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }

    /** Ghi lỗi hệ thống nghiêm trọng kèm chuỗi dấu vết (Stack Trace) */
    public static void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }

    /** Ghi thông báo dùng để gỡ lỗi (Debug) */
    public static void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    /**
     * Xóa sạch toàn bộ nội dung trong file log.
     * Thường dùng khi file log quá lớn hoặc khởi động dự án mới.
     */
    public static void clearLog() {
        if (!initialized) {
            initialize();
        }

        try {
            if (logFilePath != null && Files.exists(logFilePath)) {
                Files.writeString(logFilePath, "");
                log(LogLevel.INFO, "Tệp nhật ký (Log file) đã được dọn sạch.");
            }
        } catch (IOException e) {
            System.err.println("Dọn dẹp file log thất bại: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lấy đường dẫn vật lý của file log trên ổ cứng.
     */
    public static Path getLogFilePath() {
        if (!initialized) {
            initialize();
        }
        return logFilePath;
    }
}