package com.disaster.analysis.infrastructure.repository.context;

import com.disaster.analysis.util.LogUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Cổng kết nối trung tâm (Connection Gateway).
 * Quản lý cấu hình từ .env và cung cấp các luồng kết nối phân cấp.
 */
public class DbContext {

    private static String rootUrl;
    private static String cachedUrl;
    private static String dbName;
    private static String dbUser;
    private static String dbPassword;

    // Khai báo DataSource của Hikari
    private static HikariDataSource dataSource;

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

            // KHỞI TẠO CONNECTION POOL
            initConnectionPool();

        } catch (Exception e) {
            LogUtil.error("DbContext Static Block Initialization Failed: " + e.getMessage());
        }
    }

    private static void initConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(cachedUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);

        // Cấu hình tối ưu cho HikariCP
        config.setMaximumPoolSize(10); // Số lượng kết nối tối đa giữ trong Pool (thường 10-20 là đủ cho ứng dụng vừa)
        config.setMinimumIdle(2);      // Số kết nối rảnh rỗi tối thiểu luôn duy trì
        config.setConnectionTimeout(30000); // Đợi tối đa 30s nếu hết kết nối rảnh
        config.setIdleTimeout(600000); // Đóng kết nối nếu rảnh quá 10 phút để giải phóng RAM
        config.setMaxLifetime(1800000); // Tuổi thọ tối đa của 1 kết nối (30 phút)

        dataSource = new HikariDataSource(config);
        LogUtil.info("HikariCP Connection Pool initialized successfully.");
    }

    /**
     * Dành cho Repository: Giờ đây sẽ "mượn" kết nối từ Pool thay vì tạo mới.
     * Tốc độ truy xuất sẽ tăng lên đáng kể.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("Connection Pool is not initialized.");
        return dataSource.getConnection();
    }

    /**
     * Dành riêng cho Initializer: Vẫn dùng DriverManager vì chỉ chạy 1 lần lúc startup.
     */
    public static Connection getRootConnection() throws SQLException {
        if (rootUrl == null) throw new SQLException("Invalid root URL configuration.");
        return DriverManager.getConnection(rootUrl, dbUser, dbPassword);
    }

    public static String getDbName() {
        return dbName;
    }

    /**
     * Hàm dùng để đóng Pool khi tắt ứng dụng (tránh Memory Leak)
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LogUtil.info("Connection Pool closed.");
        }
    }
}
