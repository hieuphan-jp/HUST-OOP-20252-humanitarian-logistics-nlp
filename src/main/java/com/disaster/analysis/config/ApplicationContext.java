package com.disaster.analysis.config;

// Nhập các đối tượng truyền dữ liệu (DTO) và cấu trúc lõi
import com.disaster.analysis.application.dto.ProjectDTO;
import com.disaster.analysis.domain.model.enums.Platform;

// Nhập các Interface (Cổng giao tiếp) định nghĩa hợp đồng của hệ thống
import com.disaster.analysis.domain.contract.social.DataSource;
import com.disaster.analysis.domain.contract.analysis.DamageClassifier;
import com.disaster.analysis.domain.contract.analysis.SentimentAnalyzer;
import com.disaster.analysis.domain.contract.preprocessing.TextPreprocessor;
import com.disaster.analysis.domain.contract.export.Exporter;
import com.disaster.analysis.domain.contract.ai.AIClient;

// Nhập các lớp thực thi (Implementation) nằm ở tầng Hạ tầng (Infrastructure)
import com.disaster.analysis.infrastructure.preprocessing.TextPreprocessorImpl;
import com.disaster.analysis.infrastructure.social.*;
import com.disaster.analysis.infrastructure.ai.*;
import com.disaster.analysis.infrastructure.persistence.*;
import com.disaster.analysis.infrastructure.analysis.*;
import com.disaster.analysis.infrastructure.export.XlsxExporter;

// Nhập các Service (Tầng ứng dụng - Application Layer) xử lý nghiệp vụ
import com.disaster.analysis.application.service.*;

// Nhập các Interface Repository để thao tác với Cơ sở dữ liệu
import com.disaster.analysis.domain.contract.repository.*;

// Nhập công cụ đọc file cấu hình môi trường (.env) và Ghi log
import com.disaster.analysis.util.LogUtil;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.HashMap;
import java.util.Map;

/**
 * Lớp Quản lý Ngữ cảnh Ứng dụng (Application Context).
 * Áp dụng mẫu thiết kế Singleton và Dependency Injection (Cấp phát phụ thuộc) thủ công.
 * Đóng vai trò là trung tâm khởi tạo và kết nối mọi thành phần của dự án lại với nhau.
 */
public class ApplicationContext {

    // Biến tĩnh lưu trữ phiên bản (instance) duy nhất của toàn hệ thống (Singleton Pattern)
    private static ApplicationContext instance;

    // Trạng thái lõi của ứng dụng: Lưu trữ phiên làm việc của Dự án đang được chọn trên giao diện
    private ProjectDTO currentProject;

    // Khai báo các biến chứa phiên bản duy nhất (Singleton Beans) của các Service nghiệp vụ
    private ProjectService projectService;
    private DataCollectionService dataCollectionService;
    private SentimentAnalysisService sentimentAnalysisService;
    private DamageClassificationService damageClassificationService;
    private AISummaryService aiSummaryService;
    private ExportService exportService;

    // Kho lưu trữ đa năng dạng Key-Value để chứa các cấu hình động của hệ thống
    private final Map<String, Object> properties;

    /**
     * Hàm khởi tạo riêng tư (private) để ngăn chặn việc dùng từ khóa 'new' tạo đối tượng từ bên ngoài.
     */
    private ApplicationContext() {
        this.properties = new HashMap<>(); // Khởi tạo kho lưu trữ cấu hình trống
    }

    /**
     * Cung cấp điểm truy cập để lấy instance duy nhất của ApplicationContext.
     * Sử dụng synchronized để đảm bảo an toàn tuyệt đối khi chạy đa luồng (Thread-safe).
     */
    public static synchronized ApplicationContext getInstance() {
        if (instance == null) { // Nếu hệ thống chưa từng tạo Context nào
            instance = new ApplicationContext(); // Thì tiến hành khởi tạo lần đầu tiên
        }
        return instance; // Trả về instance duy nhất
    }

    /**
     * Hàm quan trọng nhất: Khởi tạo và lắp ráp toàn bộ bộ máy hệ thống theo đúng thứ tự phụ thuộc.
     */
    public void initialize() {
        LogUtil.info("Starting ApplicationContext initialization pipeline...");

        // BƯỚC 1: KHỞI TẠO TẦNG LƯU TRỮ (REPOSITORIES)
        ProjectRepository projectRepository = new ProjectRepositoryImpl();
        PostRepository postRepository = new PostRepositoryImpl();
        CommentRepository commentRepository = new CommentRepositoryImpl();
        SummaryRepository summaryRepository = new SummaryRepositoryImpl();

        // BƯỚC 2: KHỞI TẠO CÁC CÔNG CỤ XỬ LÝ LÕI (PORTS & ADAPTERS)
        TextPreprocessor textPreprocessor = new TextPreprocessorImpl(); // Công cụ làm sạch văn bản
        SentimentAnalyzer sentimentAnalyzer = new HybridSentimentAnalyzer(); // Công cụ chấm điểm cảm xúc AI
        DamageClassifier damageClassifier = new KeywordDamageClassifier(); // Công cụ bóc tách nhãn thiệt hại
        Exporter exporter = new XlsxExporter(); // Công cụ xuất file Excel

        // BƯỚC 3: KHỞI TẠO TẦNG DỊCH VỤ NGHIỆP VỤ (SERVICES) VÀ BƠM PHỤ THUỘC (DEPENDENCY INJECTION)

        // Khởi tạo ProjectService (Cần có các kho lưu trữ để thực hiện CRUD Dự án)
        this.projectService = new ProjectService(projectRepository, postRepository, commentRepository);

        // Khởi tạo PreprocessingService
        PreprocessingService preprocessingService = new PreprocessingService(
                textPreprocessor,
                postRepository,
                commentRepository
        );

        // Nạp và cấu hình danh sách các bộ cào dữ liệu mạng xã hội (DataSource)
        Map<Platform, DataSource> dataSources = createDataSources();

        // Khởi tạo DataCollectionService
        this.dataCollectionService = new DataCollectionService(
                postRepository,
                commentRepository,
                preprocessingService,
                dataSources,
                projectRepository
        );

        // Khởi tạo và kích hoạt Service phân tích cảm xúc
        this.sentimentAnalysisService = new SentimentAnalysisService(
                sentimentAnalyzer,
                postRepository,
                commentRepository
        );
        sentimentAnalysisService.initialize();

        // Khởi tạo và kích hoạt Service phân loại thiệt hại
        this.damageClassificationService = new DamageClassificationService(
                damageClassifier,
                postRepository,
                commentRepository
        );
        damageClassificationService.initialize();

        // BƯỚC 4: KHỞI TẠO CÁC DỊCH VỤ BẬC CAO TỔNG HỢP

        // Cấp phát Client AI thực sự (Gemini) hoặc Client giả lập
        AIClient aiClient = createAIClient();

        // CẬP NHẬT CHUẨN XÁC: Khởi tạo bộ máy làm báo cáo AI với đúng 5 tham số theo cấu trúc mới của AISummaryService
        this.aiSummaryService = new AISummaryService(
                aiClient,
                summaryRepository,
                postRepository,
                commentRepository,
                projectRepository
        );

        // Khởi tạo dịch vụ xuất dữ liệu ra file Excel
        this.exportService = new ExportService(
                postRepository,
                commentRepository,
                exporter
        );

        LogUtil.info("ApplicationContext initialized successfully. All dependencies wired up.");
    }

    /**
     * Ủy quyền việc quyết định khởi tạo AI cho lớp AIFactory.
     */
    private AIClient createAIClient() {
        return AIFactory.getActiveClient();
    }

    /**
     * Khởi tạo bản đồ chứa các bộ cào dữ liệu (DataSources) tương ứng với từng mạng xã hội.
     */
    private Map<Platform, DataSource> createDataSources() {
        Map<Platform, DataSource> dataSources = new HashMap<>();

        Dotenv dotenv = Dotenv.load();

        String youtubeApiKey = dotenv.get("YOUTUBE_API_KEY", "");

        if (youtubeApiKey != null && !youtubeApiKey.isEmpty()) {
            dataSources.put(Platform.YOUTUBE, new YouTubeDataSource(youtubeApiKey));
            LogUtil.info("YouTube DataSource injected with real API Key.");
        } else {
            dataSources.put(Platform.YOUTUBE, new MockDataSource(Platform.YOUTUBE));
            LogUtil.warn("YouTube API Key missing. Falling back to MockDataSource.");
        }

        dataSources.put(Platform.REDDIT, new RedditDataSource());
        dataSources.put(Platform.NEWS, new NewsDataSource());

        return dataSources;
    }


    // GETTER / SETTER ĐỂ TRUY XUẤT CÁC SERVICE TỪ GIAO DIỆN UI
    public ProjectDTO getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(ProjectDTO project) {
        this.currentProject = project;
    }

    public ProjectService getProjectService() {
        if (projectService == null) throw new IllegalStateException("ApplicationContext not initialized!");
        return projectService;
    }

    public DataCollectionService getDataCollectionService() {
        if (dataCollectionService == null) throw new IllegalStateException("ApplicationContext not initialized!");
        return dataCollectionService;
    }

    public SentimentAnalysisService getSentimentAnalysisService() {
        if (sentimentAnalysisService == null) throw new IllegalStateException("ApplicationContext not initialized!");
        return sentimentAnalysisService;
    }

    public DamageClassificationService getDamageClassificationService() {
        if (damageClassificationService == null) throw new IllegalStateException("ApplicationContext not initialized!");
        return damageClassificationService;
    }

    public AISummaryService getAISummaryService() {
        if (aiSummaryService == null) throw new IllegalStateException("ApplicationContext not initialized!");
        return aiSummaryService;
    }

    public ExportService getExportService() {
        if (exportService == null) throw new IllegalStateException("ApplicationContext not initialized!");
        return exportService;
    }

    // KHỐI QUẢN LÝ THUỘC TÍNH CẤU HÌNH ĐỘNG (PROPERTIES)
    public void setProperty(String key, Object value) {
        if (key == null) throw new IllegalArgumentException("Property key cannot be null");
        properties.put(key, value);
    }

    public Object getProperty(String key) {
        if (key == null) throw new IllegalArgumentException("Property key cannot be null");
        return properties.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> type) {
        if (key == null) throw new IllegalArgumentException("Property key cannot be null");
        if (type == null) throw new IllegalArgumentException("Type cannot be null");

        Object value = properties.get(key);
        if (value == null) return null;

        if (!type.isInstance(value)) {
            throw new ClassCastException(
                    "Property '" + key + "' is of type " + value.getClass().getName() +
                            ", cannot cast to " + type.getName()
            );
        }

        return (T) value;
    }

    public boolean hasProperty(String key) {
        if (key == null) throw new IllegalArgumentException("Property key cannot be null");
        return properties.containsKey(key);
    }

    public Object removeProperty(String key) {
        if (key == null) throw new IllegalArgumentException("Property key cannot be null");
        return properties.remove(key);
    }

    public void clear() {
        this.currentProject = null;
        this.properties.clear();
        LogUtil.info("ApplicationContext properties and session cleared.");
    }
}