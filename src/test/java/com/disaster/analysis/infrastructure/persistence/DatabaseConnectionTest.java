package com.disaster.analysis.infrastructure.persistence;

import com.disaster.analysis.domain.contract.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionTest {

    @Test
    public void testSqlServerConnectionAndRead() {
        // 1. Chuẩn bị (Arrange)
        // Khởi tạo Repository - Bước này sẽ ngầm gọi cấu hình DatabaseConfig của bạn
        ProjectRepository projectRepository = new ProjectRepositoryImpl();

        // 2. Thực thi & Khẳng định (Act & Assert)
        // Dùng assertDoesNotThrow để đảm bảo rằng lệnh gọi xuống DB không bị văng lỗi vỡ mặt (Exception)
        assertDoesNotThrow(() -> {

            // Gọi thử một lệnh SELECT cơ bản nhất
            var projects = projectRepository.findAll();

            // Nếu chạy qua được dòng trên mà không sập, chứng tỏ SQL Server ĐANG SỐNG!
            assertNotNull(projects, "Lỗi: Hàm findAll trả về null thay vì danh sách.");

            System.out.println("✅ Kết nối SQL Server THÀNH CÔNG! Đọc được " + projects.size() + " dự án.");

        }, "❌ LỖI NGHIÊM TRỌNG: Không thể kết nối tới SQL Server. Hãy kiểm tra lại file cấu hình hoặc xem Service SQL đã bật chưa!");
    }
}