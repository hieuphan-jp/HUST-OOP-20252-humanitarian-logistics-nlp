package com.disaster.analysis.infrastructure.repository.context;

import com.disaster.analysis.util.LogUtil;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Cổng kết nối trung tâm (Connection Gateway).
 * Quản lý cấu hình từ .env và cung cấp các luồng kết nối phân cấp.
 */
public class DbContext {

    private static String rootUrl;    // URL trỏ vào máy chủ gốc (Master)
    private static String cachedUrl;  // URL trỏ vào đúng Database nghiệp vụ
    private static String dbName;
    private static String dbUser;
    private static String dbPassword;

    static {
        try {
            Dotenv dotenv = Dotenv.load();
            rootUrl = dotenv.get("DB_URL");
            dbName = dotenv.get("DB_NAME");
            dbUser = dotenv.get("DB_USERNAME");
            dbPassword = dotenv.get("DB_PASSWORD");

            if (rootUrl != null && dbName != null && !rootUrl.contains("databaseName")) {
                cachedUrl = rootUrl + ";databaseName=" + dbName;
            } else {
                cachedUrl = rootUrl;
            }

            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (Exception e) {
            LogUtil.error("DbContext Static Block Initialization Failed: " + e.getMessage());
        }
    }

    /**
     * Dành cho Repository: Trả về kết nối đích đã nhúng sẵn tên Database.
     */
    public static Connection getConnection() throws SQLException {
        if (cachedUrl == null) throw new SQLException("Invalid database configuration.");
        return DriverManager.getConnection(cachedUrl, dbUser, dbPassword);
    }

    /**
     * Dành riêng cho Initializer: Trả về kết nối gốc để kiểm tra và tạo Database mới.
     */
    public static Connection getRootConnection() throws SQLException {
        if (rootUrl == null) throw new SQLException("Invalid root URL configuration.");
        return DriverManager.getConnection(rootUrl, dbUser, dbPassword);
    }

    /**
     * Cung cấp tên Database cho các lớp khác nếu cần dùng (như Initializer).
     */
    public static String getDbName() {
        return dbName;
    }
}