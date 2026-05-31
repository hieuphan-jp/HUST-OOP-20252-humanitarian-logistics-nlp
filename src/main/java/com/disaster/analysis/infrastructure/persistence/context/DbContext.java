package com.disaster.analysis.infrastructure.persistence.context;

import com.disaster.analysis.domain.exception.DatabaseException;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Lớp cấu hình trung tâm quản lý kết nối cơ sở dữ liệu (Database Context).
 * Thực hiện nhiệm vụ nạp cấu hình từ môi trường (.env) và cấp phát luồng kết nối JDBC.
 */
public class DbContext {

    // Nạp tĩnh (static) cấu hình từ file .env một lần duy nhất khi ứng dụng khởi động
    private static final Dotenv dotenv = Dotenv.load();
    private static final String URL = dotenv.get("DB_URL");
    private static final String USERNAME = dotenv.get("DB_USERNAME");
    private static final String PASSWORD = dotenv.get("DB_PASSWORD");

    /**
     * Khởi tạo private để ngăn chặn việc tạo đối tượng DbContext từ bên ngoài bằng từ khóa 'new'.
     */
    private DbContext() {}

    /**
     * Khởi tạo và trả về một kết nối vật lý hoạt động tới cơ sở dữ liệu.
     * Tự động cấu hình dựa trên chuỗi kết nối nhận diện từ file môi trường.
     *
     * @return {@link Connection} Luồng kết nối cơ sở dữ liệu sẵn sàng thực thi lệnh.
     * @throws DatabaseException Nếu thông tin cấu hình thiếu hoặc không thể thiết lập liên kết mạng.
     */
    public static Connection getConnection() {
        try {
            // Kiểm tra tính đầy đủ của file cấu hình hệ thống
            if (URL == null || USERNAME == null || PASSWORD == null) {
                throw new DatabaseException("Lỗi cấu hình: Không tìm thấy thông tin DB_URL, DB_USERNAME hoặc DB_PASSWORD trong file .env");
            }

            // Trả về luồng kết nối chuẩn của Java JDBC
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);

        } catch (SQLException e) {
            // Bọc lỗi SQLException của Java thành DatabaseException của hệ thống để đảm bảo tính tinh khiết của tầng Domain
            throw new DatabaseException("Thất bại khi kết nối tới hệ quản trị cơ sở dữ liệu. Hãy đảm bảo thông tin tài khoản chính xác và dịch vụ đã được khởi động.", e);
        }
    }
}