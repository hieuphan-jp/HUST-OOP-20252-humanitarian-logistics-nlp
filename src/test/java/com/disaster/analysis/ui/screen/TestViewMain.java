package com.disaster.analysis.ui.screen;

import com.disaster.analysis.ui.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class TestViewMain extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Chỉ định đường dẫn đến file main.fxml (đảm bảo đúng cấu hình thư mục resources)
            // Thường file fxml sẽ nằm trong src/main/resources/com/disaster/analysis/ui/main.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("resources/main.fxml"));

            Parent root = loader.load();

            // 2. Lấy ra Controller của main.fxml nếu bạn muốn cấu hình hoặc inject dữ liệu giả (Mock Data) để test
            MainController controller = loader.getController();
            // controller.initData(...); // Ví dụ nạp dữ liệu giả vào TableView để test hiển thị

            // 3. Khởi tạo Scene và thiết lập kích thước cửa sổ test
            Scene scene = new Scene(root, 1000, 600); // Bạn có thể tùy chỉnh kích thước mong muốn

            primaryStage.setTitle("Test View Main - Disaster Social Media Analysis");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen(); // Đẩy cửa sổ ra giữa màn hình khi bật
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy hoặc không thể load file main.fxml. Hãy kiểm tra lại đường dẫn getResource!");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Kích hoạt ứng dụng JavaFX
        launch(args);
    }
}