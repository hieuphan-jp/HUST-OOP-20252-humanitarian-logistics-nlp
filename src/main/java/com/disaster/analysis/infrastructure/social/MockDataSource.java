package com.disaster.analysis.infrastructure.social; // Định vị package nằm trong tầng giao tiếp hạ tầng (Infrastructure)

// Nhập các interface và class cốt lõi từ tầng nghiệp vụ (Domain) để liên kết hệ thống
import com.disaster.analysis.domain.contract.social.DataSource; // Interface gốc định nghĩa các hàm cào dữ liệu
import com.disaster.analysis.domain.exception.DataSourceException; // Lớp bắt lỗi chuyên biệt khi cào dữ liệu
import com.disaster.analysis.domain.model.entities.Comment; // Thực thể lưu trữ dữ liệu bình luận
import com.disaster.analysis.domain.model.entities.Post; // Thực thể lưu trữ dữ liệu bài đăng mạng xã hội
import com.disaster.analysis.domain.model.enums.DamageCategory; // Enum định nghĩa các loại thiệt hại (Ví dụ: Sập nhà, thương vong)
import com.disaster.analysis.domain.model.enums.Platform; // Enum định nghĩa các mạng xã hội (YOUTUBE, NEWS, REDDIT...)
import com.disaster.analysis.domain.model.enums.Sentiment; // Enum định nghĩa thang điểm cảm xúc (Tích cực, Tiêu cực, Trung lập)

// Nhập các thư viện tiện ích chuẩn của Java phục vụ tính toán thời gian, mảng và ép chuỗi
import java.time.LocalDateTime; // Thư viện quản lý ngày giờ hệ thống
import java.time.temporal.ChronoUnit; // Công cụ tính toán khoảng cách giữa hai mốc thời gian (ngày, giờ, phút)
import java.util.*; // Nhập toàn bộ cấu trúc dữ liệu như List, Set, Map, ArrayList, HashSet, HashMap
import java.util.concurrent.ThreadLocalRandom; // Bộ sinh số ngẫu nhiên tối ưu cao, an toàn khi chạy đa luồng
import java.util.stream.Collectors; // Công cụ chuyển đổi luồng dữ liệu (Stream API) sang cấu trúc mảng hoặc chuỗi

/**
 * Bộ sinh dữ liệu giả lập (Mock DataSource).
 * Tự động tạo ra các bài viết và bình luận Tiếng Việt giống hệt mạng xã hội thực tế
 * để phục vụ quá trình test UI và logic Backend mà không cần gọi API thật hoặc tốn chi phí mạng.
 */
public class MockDataSource implements DataSource { // Kế thừa từ Interface DataSource để đóng giả làm một bộ cào thật

    private final Platform platform; // Biến lưu trữ nền tảng hiện tại mà class này đang giả lập
    private final Random random; // Đối tượng dùng để bốc thăm ngẫu nhiên các mẫu câu chữ

    // Khai báo mảng tĩnh chứa tiêu đề các trận thiên tai thực tế tại Việt Nam để chèn vào văn bản
    private static final String[] DISASTER_NAMES = {
            "Siêu bão Yagi", "Bão số 3", "Hoàn lưu bão Yagi",
            "Lũ lụt miền Bắc", "Sạt lở đất tại Lào Cai", "Lũ quét Yên Bái",
            "Bão Noru", "Mưa lũ miền Trung"
    };

    // Khai báo mảng chứa các kịch bản văn bản Tích cực (Thể hiện sự lạc quan, đùm bọc, cứu trợ)
    private static final String[] POSITIVE_TEMPLATES = {
            "Công tác cứu trợ cho %s đang diễn ra rất nhanh chóng! Cả nước đang hướng về bà con.",
            "Thật biết ơn các anh bộ đội và tình nguyện viên đã không quản ngày đêm giúp đỡ nạn nhân %s.",
            "Tin vui: Hàng chục chuyến xe chở nhu yếu phẩm đã tiếp cận được vùng rốn lũ %s.",
            "Tuyệt vời tinh thần tương thân tương ái của người Việt Nam trong %s! Cố lên đồng bào ơi.",
            "Nước đã bắt đầu rút sau %s, bà con đang dọn dẹp nhà cửa. Mong mọi thứ sớm bình yên.",
            "Đã cứu thành công nhiều hộ gia đình bị mắc kẹt do %s. Thật sự nể phục lực lượng cứu hộ!",
            "Chính quyền địa phương phản ứng rất kịp thời trong đợt %s này. Mọi người đã được di dời đến nơi an toàn."
    };

    // Khai báo mảng chứa các kịch bản văn bản Tiêu cực (Thể hiện sự hoảng sợ, tang thương, mất mát)
    private static final String[] NEGATIVE_TEMPLATES = {
            "Hậu quả từ %s quá tàn khốc. Hàng ngàn ngôi nhà bị ngập tới nóc, người dân bơ vơ.",
            "Đau xót quá, %s đã cướp đi sinh mạng của bao nhiêu người. Khung cảnh thật tang thương.",
            "Hiện tại vùng này đang bị cô lập hoàn toàn do %s, mất điện, mất sóng, cạn kiệt thức ăn.",
            "Nước lên nhanh quá trở tay không kịp! %s phá hủy toàn bộ tài sản và hoa màu của bà con rồi.",
            "Kêu cứu: Cần lắm ca nô và áo phao! Khu vực tôi đang bị ngập sâu sau %s.",
            "Ám ảnh thực sự. Cả một mảng đồi sạt lở vùi lấp bao nhiêu nhà dân trong đợt %s vừa rồi.",
            "Đường sá nát bét, sạt lở khắp nơi do %s. Thiệt hại kinh tế đợt này chắc chắn rất khủng khiếp."
    };

    // Khai báo mảng chứa các kịch bản văn bản Trung lập (Cập nhật số liệu, tin tức thuần túy, không cảm xúc)
    private static final String[] NEUTRAL_TEMPLATES = {
            "Cập nhật tình hình %s: Cơ quan khí tượng dự báo mưa lớn sẽ còn tiếp diễn trong 2 ngày tới.",
            "Báo cáo nhanh: Thống kê ban đầu cho thấy %s đã làm ảnh hưởng đến hơn 50.000 hộ dân.",
            "Chính quyền đang họp khẩn để đưa ra phương án khắc phục hậu quả của %s.",
            "Tổng hợp danh sách các tuyến đường đang bị phong tỏa do ảnh hưởng của %s để mọi người né tránh.",
            "Thông tin tài khoản chính thức của Mặt trận Tổ quốc để quyên góp cho đồng bào chịu ảnh hưởng bởi %s.",
            "Họp báo chính phủ công bố gói ngân sách hỗ trợ tái thiết cơ sở hạ tầng sau %s.",
            "Nhiều trường học thông báo cho học sinh nghỉ học để đảm bảo an toàn trong đợt %s."
    };

    // Tạo bản đồ Map liên kết giữa một danh mục thiệt hại (Enum) với các từ khóa tiếng Việt đặc trưng
    private static final Map<DamageCategory, String[]> DAMAGE_KEYWORDS = new HashMap<>();
    static { // Khối static chạy ngay khi class được nạp vào bộ nhớ để điền dữ liệu vào bản đồ Map
        // Liên kết danh mục ảnh hưởng con người với các từ khóa thương vong
        DAMAGE_KEYWORDS.put(DamageCategory.PEOPLE_AFFECTED, new String[]{
                "người bị thương", "nạn nhân", "người mất tích", "bà con mắc kẹt", "người tử vong"
        });
        // Liên kết danh mục đình trệ kinh tế với các từ khóa kinh doanh đóng cửa
        DAMAGE_KEYWORDS.put(DamageCategory.ECONOMIC_DISRUPTION, new String[]{
                "doanh nghiệp điêu đứng", "hàng quán đóng cửa", "chuỗi cung ứng đứt gãy", "thiệt hại kinh tế nặng"
        });
        // Liên kết danh mục phá hủy nhà cửa với các cụm từ đổ sập, tốc mái
        DAMAGE_KEYWORDS.put(DamageCategory.BUILDING_DAMAGE, new String[]{
                "nhà cửa đổ sập", "tốc mái", "trường học tốc mái", "nhà bị lũ cuốn trôi", "công trình hư hỏng"
        });
        // Liên kết danh mục mất tài sản cá nhân với từ khóa ngập xe cộ, trôi đồ đạc
        DAMAGE_KEYWORDS.put(DamageCategory.PERSONAL_PROPERTY_LOSS, new String[]{
                "trôi hết xe cộ", "tài sản mất sạch", "đồ đạc ngập trong nước", "mất trắng lợn gà"
        });
        // Liên kết danh mục hạ tầng công cộng với từ khóa mất điện, sập cầu, sạt lở quốc lộ
        DAMAGE_KEYWORDS.put(DamageCategory.INFRASTRUCTURE_DAMAGE, new String[]{
                "sập cầu", "đứt cáp quang", "mất điện diện rộng", "sạt lở quốc lộ", "mất nước sạch"
        });
        // Liên kết danh mục khác với các từ khóa nông nghiệp ngập úng
        DAMAGE_KEYWORDS.put(DamageCategory.OTHER, new String[]{
                "mất trắng mùa màng", "cây xanh gãy đổ la liệt", "môi trường ô nhiễm", "ngập úng lúa"
        });
    }

    // Khai báo mảng chứa tên các đơn vị/người đăng tin lớn đóng vai trò là Author của bài viết
    private static final String[] AUTHORS = {
            "Phóng Viên Thường Trú", "Người Dân Địa Phương", "Đội Cứu Hộ", "Ban Chỉ Đạo Phòng Chống Thiên Tai",
            "Trang Tin 24h", "Hội Chữ Thập Đỏ VN", "Tình Nguyện Viên", "Mặt Trận Tổ Quốc", "Tin Tức VTV"
    };

    // Khai báo mảng chứa mẫu bình luận mang tính động viên tích cực
    private static final String[] POSITIVE_COMMENT_TEMPLATES = {
            "Cảm ơn bạn đã cập nhật thông tin. Cầu mong mọi người bình an!",
            "Tuyệt vời quá, cảm ơn các mạnh thường quân.",
            "Ấm lòng thực sự. Miền Nam luôn hướng về miền Bắc thân yêu!",
            "Mình có một thuyền máy nhỏ, liên hệ ai để tham gia cứu hộ đây ạ?",
            "Đã chuyển khoản ủng hộ quỹ. Mong bà con sớm vượt qua khó khăn."
    };

    // Khai báo mảng chứa mẫu bình luận đau xót, lo lắng tiêu cực
    private static final String[] NEGATIVE_COMMENT_TEMPLATES = {
            "Nhìn video mà xót xa quá rơi nước mắt luôn.",
            "Trời ơi sập hết nhà cửa thế kia thì sống sao đây trời?",
            "Chỗ nhà ngoại em đang ngập đến nóc rồi, gọi cứu hộ mà chưa thấy tới. Lo quá!",
            "Thiên nhiên nổi giận thật đáng sợ. Mất mát này quá lớn.",
            "Cảnh tượng kinh hoàng thực sự, năm nay thiên tai khắc nghiệt quá."
    };

    // Khai báo mảng chứa mẫu bình luận hỏi đáp thông tin trung lập
    private static final String[] NEUTRAL_COMMENT_TEMPLATES = {
            "Cho mình xin thông tin liên hệ của nhóm cứu trợ khu vực này với ạ.",
            "Hiện tại quốc lộ có đi được không mọi người?",
            "Có ai biết tình hình ở huyện Trấn Yên sao rồi không ạ?",
            "Đã share bài viết để nhiều người biết tới thông tin này.",
            "Chỗ mình đang cúp điện, ai có số tổng đài điện lực cho mình xin."
    };

    // Mảng chứa tên tài khoản người dùng mạng xã hội ngẫu nhiên đóng vai trò là Author của bình luận
    private static final String[] COMMENT_AUTHORS = {
            "Nguyen Van A", "Tran Thi B", "Nguoi_Qua_Duong", "Hoang Lan",
            "Thanh Tung", "Lan Anh 99", "Phuot_Thu", "Hanh_Phuc_Quanh_Ta",
            "Dung_Xe_Khach", "Thao_My_Store"
    };

    /**
     * Hàm khởi tạo có tham số, tiêm (inject) nền tảng MXH cụ thể cần đóng vai giả lập.
     */
    public MockDataSource(Platform platform) {
        this.platform = platform; // Gán nền tảng được truyền vào cho biến nội bộ
        this.random = new Random(); // Khởi tạo bộ sinh số ngẫu nhiên riêng cho đối tượng này
    }

    /**
     * Hàm khởi tạo mặc định không tham số, tự động chọn YOUTUBE làm nền tảng mặc định.
     */
    public MockDataSource() {
        this(Platform.YOUTUBE); // Gọi lại hàm khởi tạo có tham số phía trên và truyền vào Platform.YOUTUBE
    }

    /**
     * Hàm giả lập cào danh sách bài viết từ thanh tìm kiếm.
     */
    @Override
    public List<Post> fetchPosts(String query, LocalDateTime startDate,
                                 LocalDateTime endDate, int maxResults) throws DataSourceException {

        // Gác cổng 1: Kiểm tra nếu từ khóa truyền vào bị rỗng thì văng lỗi hệ thống ngay lập tức
        if (query == null || query.trim().isEmpty()) {
            throw new DataSourceException(platform, "Từ khóa tìm kiếm không được để trống.");
        }
        // Gác cổng 2: Kiểm tra nếu mốc thời gian bị null thì báo lỗi không thực thi được
        if (startDate == null || endDate == null) {
            throw new DataSourceException(platform, "Ngày bắt đầu và kết thúc không được để trống.");
        }
        // Gác cổng 3: Kiểm tra tính logic thời gian, ngày bắt đầu không được lớn hơn ngày kết thúc
        if (startDate.isAfter(endDate)) {
            throw new DataSourceException(platform, "Ngày bắt đầu không được lớn hơn ngày kết thúc.");
        }

        List<Post> posts = new ArrayList<>(); // Khởi tạo mảng động để chứa danh sách các bài viết giả lập trả về
        // Công thức tính số lượng bài viết sinh ra ngẫu nhiên dao động từ 50% đến 100% của chỉ tiêu maxResults
        int numPosts = maxResults / 2 + random.nextInt(maxResults / 2 + 1);

        // Chạy vòng lặp duyệt qua số lượng bài viết cần sinh ra
        for (int i = 0; i < numPosts; i++) {
            posts.add(generateMockPost(query, startDate, endDate)); // Gọi hàm trộn chữ tạo Post và nhét vào danh sách kết quả
        }
        return posts; // Trả về danh sách bài viết đã sinh xong cho hệ thống sử dụng
    }

    /**
     * Hàm giả lập cào danh sách bình luận bên dưới một bài viết cho trước.
     */
    @Override
    public List<Comment> fetchComments(Post post, int maxResults) throws DataSourceException {
        List<Comment> comments = new ArrayList<>(); // Khởi tạo mảng động chứa danh sách bình luận
        // Công thức tính số bình luận ngẫu nhiên đổ về dao động từ 30% đến 100% của chỉ tiêu maxResults
        int numComments = (maxResults * 3) / 10 + random.nextInt((maxResults * 7) / 10 + 1);
        // Trích xuất từ khóa thiên tai có sẵn trong bài đăng gốc để bình luận bên dưới ăn khớp ngữ cảnh
        String disasterContext = extractDisasterName(post.getContent());

        // Chạy vòng lặp sinh bình luận dựa theo số lượng đã tính toán ở trên
        for (int i = 0; i < numComments; i++) {
            comments.add(generateMockComment(post, disasterContext)); // Gọi hàm trộn tạo bình luận giả và nhét vào mảng
        }
        return comments; // Trả về danh sách bình luận đã trộn xong
    }

    /**
     * Trả về định danh nền tảng mà driver này đang đảm nhận.
     */
    @Override
    public Platform getPlatform() {
        return platform; // Trả về giá trị của biến platform (Ví dụ: Platform.YOUTUBE)
    }

    // KHỐI SINH BÀI VIẾT (POST) GIẢ LẬP
    private Post generateMockPost(String query, LocalDateTime startDate, LocalDateTime endDate) {
        // Tính toán tổng số ngày chênh lệch giữa mốc bắt đầu và mốc kết thúc của dự án
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        // Lấy ngẫu nhiên một số ngày nằm trong khoảng chênh lệch trên (bảo vệ tránh lỗi nếu khoảng cách bằng 0)
        long randomDays = daysBetween > 0 ? ThreadLocalRandom.current().nextLong(daysBetween + 1) : 0;
        // Tịnh tiến ngày bắt đầu cộng thêm số ngày ngẫu nhiên, giờ ngẫu nhiên và phút ngẫu nhiên để tạo mốc thời gian đăng bài tự nhiên
        LocalDateTime publishedAt = startDate.plusDays(randomDays)
                .plusHours(random.nextInt(24))
                .plusMinutes(random.nextInt(60));

        // Chọn ngẫu nhiên 1 trạng thái cảm xúc (Tích cực, Tiêu cực hoặc Trung lập) từ danh sách Enum Sentiment
        Sentiment sentiment = Sentiment.values()[random.nextInt(Sentiment.values().length)];
        // Gọi hàm bốc thăm ngẫu nhiên một tập hợp các danh mục thiệt hại cho bài viết này
        Set<DamageCategory> categories = generateDamageCategories();

        // Tạo ra chuỗi văn bản cốt lõi dựa trên sắc thái cảm xúc đã chọn
        String content = generateContent(sentiment);
        // Tiến hành chèn thêm các từ khóa mô tả thiệt hại tương ứng vào câu để tăng tính sinh động thực tế
        String enhancedContent = enhanceContentWithDamageKeywords(content, categories);

        // Tạo một ID bài viết giả lập duy nhất kết hợp giữa tên nền tảng và chuỗi mã hóa UUID cắt ngắn 8 ký tự
        String platformId = platform.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        // Chọn ngẫu nhiên một tác giả tin tức từ mảng AUTHORS
        String author = AUTHORS[random.nextInt(AUTHORS.length)];
        // Tạo đường dẫn URL giả lập chuẩn của bài đăng dựa theo mã ID vừa sinh ở trên
        String url = "https://" + platform.name().toLowerCase() + ".com/post/" + platformId;

        // ĐỒNG BỘ: Ép kiểu cấu trúc Set<Enum> phức tạp thành chuỗi phẳng String phân tách bằng dấu phẩy để lưu an toàn xuống DB
        String damageStr = categories.stream()
                .map(DamageCategory::name) // Lấy tên chuỗi của Enum (Ví dụ: "BUILDING_DAMAGE")
                .collect(Collectors.joining(",")); // Nối lại thành chuỗi dạng "BUILDING_DAMAGE,INFRASTRUCTURE_DAMAGE"

        Post post = new Post(); // Khởi tạo đối tượng Post rỗng
        post.setPlatformId(platformId); // Gán mã ID mạng xã hội
        post.setPlatform(platform.name()); // ĐỒNG BỘ: Điền tên Platform dạng String (Ví dụ: "YOUTUBE") thay vì truyền Object Enum
        post.setContent(enhancedContent); // Điền văn bản tiếng Việt đã được trộn từ khóa thiệt hại
        post.setAuthor(author); // Điền tên đơn vị đăng bài
        post.setPublishedAt(publishedAt); // Điền mốc thời gian đăng bài giả lập nằm trong khung thời gian dự án
        post.setUrl(url); // Điền link bài viết
        post.setDamageCategories(damageStr); // ĐỒNG BỘ: Lưu danh mục thiệt hại dạng String phẳng khớp với cấu trúc DB mới
        post.setSentiment(sentiment); // Gán nhãn cảm xúc phục vụ kiểm thử phân tích AI
        post.setCollectedAt(LocalDateTime.now()); // Gán thời gian thu thập là chính xác thời khắc hiện tại trên máy tính

        return post; // Xuất đối tượng Post hoàn chỉnh ra ngoài
    }

    // KHỐI SINH BÌNH LUẬN (COMMENT) GIẢ LẬP
    private Comment generateMockComment(Post post, String disasterContext) {
        // Giả lập thời gian đăng bình luận xuất hiện sau bài viết gốc từ vài giờ cho đến tối đa 30 ngày (24 * 30 giờ)
        long hoursAfterPost = random.nextInt(24 * 30);
        // Cộng dồn khoảng thời gian trễ này vào mốc thời gian đăng bài viết gốc để ra thời gian của bình luận
        LocalDateTime commentTime = post.getPublishedAt().plusHours(hoursAfterPost);

        // Bốc thăm ngẫu nhiên nhãn cảm xúc riêng biệt cho bình luận này (Bình luận có thể có cảm xúc ngược với bài viết)
        Sentiment sentiment = Sentiment.values()[random.nextInt(Sentiment.values().length)];
        // Gọi hàm bốc thăm danh mục thiệt hại riêng cho khối bình luận
        Set<DamageCategory> categories = generateCommentDamageCategories();

        // Trộn nội dung câu bình luận dựa theo nhãn cảm xúc và ngữ cảnh trận thiên tai từ bài đăng gốc truyền sang
        String content = generateCommentContent(sentiment, disasterContext);
        // Chèn thêm từ khóa thiệt hại chuyên biệt của bình luận vào văn bản cho tự nhiên
        String enhancedContent = enhanceContentWithDamageKeywords(content, categories);

        // Tạo mã ID bình luận giả lập có chứa hậu tố cmt gắn kèm chuỗi UUID ngẫu nhiên
        String platformId = platform.name().toLowerCase() + "_cmt_" + UUID.randomUUID().toString().substring(0, 8);
        // Bốc thăm ngẫu nhiên một tên tài khoản cá nhân từ mảng COMMENT_AUTHORS công cộng
        String author = COMMENT_AUTHORS[random.nextInt(COMMENT_AUTHORS.length)];

        // ĐỒNG BỘ: Ép kiểu danh mục thiệt hại của Comment sang chuỗi String phân tách dấu phẩy giống như bài viết
        String damageStr = categories.stream()
                .map(DamageCategory::name) // Lấy tên dạng chữ của Enum
                .collect(Collectors.joining(",")); // Kết nối lại bằng dấu phẩy

        Comment comment = new Comment(); // Khởi tạo thực thể Comment rỗng
        comment.setPostId(post.getId()); // Gán liên kết khóa ngoại với bài viết cha (Mục tiêu tối thượng khi lưu DB)
        comment.setProjectId(post.getProjectId()); // Kế thừa mã ID dự án từ bài viết cha
        comment.setPlatformId(platformId); // Gán mã ID bình luận mạng xã hội
        comment.setPlatform(platform.name()); // ĐỒNG BỘ: Gán tên nền tảng dạng String (Ví dụ: "YOUTUBE")
        comment.setContent(enhancedContent); // Gán văn bản tiếng Việt của bình luận
        comment.setAuthor(author); // Gán tên người bình luận
        comment.setPublishedAt(commentTime); // Gán thời gian bình luận (Đảm bảo xuất hiện sau thời gian đăng bài)
        comment.setDamageCategories(damageStr); // ĐỒNG BỘ: Gán chuỗi danh mục thiệt hại dạng String phẳng
        comment.setSentiment(sentiment); // Gán nhãn cảm xúc bình luận
        comment.setCollectedAt(LocalDateTime.now()); // Gán mốc thời gian thu thập dữ liệu bằng thời gian hệ thống thực tế

        return comment; // Xuất đối tượng Comment hoàn chỉnh ra ngoài
    }


    // CÁC HÀM TIỆN ÍCH TRỘN VĂN BẢN (TEXT MANIPULATION UTILITIES)
    /**
     * Trộn mẫu câu bài viết dựa theo sắc thái cảm xúc mong muốn.
     */
    private String generateContent(Sentiment sentiment) {
        // Sử dụng cú pháp switch-case biểu thức mới để chọn mảng mẫu câu thích hợp theo Enum Sentiment
        String[] templates = switch (sentiment) {
            case POSITIVE -> POSITIVE_TEMPLATES; // Trả về mảng mẫu câu cứu trợ nếu cảm xúc tích cực
            case NEGATIVE -> NEGATIVE_TEMPLATES; // Trả về mảng mẫu câu tàn phá nếu cảm xúc tiêu cực
            default -> NEUTRAL_TEMPLATES; // Trả về mảng cập nhật khí tượng cho trường hợp trung lập hoặc còn lại
        };
        // Bốc thăm ngẫu nhiên một mẫu câu trong mảng mẫu đã chọn
        String template = templates[random.nextInt(templates.length)];
        // Bốc thăm ngẫu nhiên một tên thảm họa từ mảng DISASTER_NAMES (Ví dụ: "Siêu bão Yagi")
        String disasterName = DISASTER_NAMES[random.nextInt(DISASTER_NAMES.length)];
        // Chèn tên thiên tai vào vị trí ký hiệu đặc biệt "%s" nằm trong mẫu câu bằng lệnh String.format
        return String.format(template, disasterName); // Trả về câu hoàn chỉnh (Ví dụ: "Hậu quả từ Siêu bão Yagi quá tàn khốc...")
    }

    /**
     * Lấy mẫu câu bình luận dựa theo sắc thái cảm xúc (Bỏ qua context thiên tai vì bình luận ngắn ít khi lặp lại tên bão).
     */
    private String generateCommentContent(Sentiment sentiment, String disasterContext) {
        // Sử dụng switch-case chọn mảng mẫu câu bình luận phù hợp với sắc thái cảm xúc tương ứng
        String[] templates = switch (sentiment) {
            case POSITIVE -> POSITIVE_COMMENT_TEMPLATES; // Các câu cảm ơn mạnh thường quân
            case NEGATIVE -> NEGATIVE_COMMENT_TEMPLATES; // Các câu khóc thương, sợ hãi
            default -> NEUTRAL_COMMENT_TEMPLATES; // Các câu xin số điện thoại, hỏi thăm thông tin đường sá
        };
        // Bốc thăm ngẫu nhiên và trả về đúng một câu bình luận đơn lẻ trong mảng mẫu câu đã chọn
        return templates[random.nextInt(templates.length)];
    }

    /**
     * Bốc thăm ngẫu nhiên một tập hợp chứa từ 1 đến 3 danh mục thiệt hại cho Bài viết gốc.
     */
    private Set<DamageCategory> generateDamageCategories() {
        Set<DamageCategory> categories = new HashSet<>(); // Sử dụng cấu hình HashSet để tự động chống trùng lặp phần tử
        DamageCategory[] allCategories = DamageCategory.values(); // Đổ toàn bộ các giá trị của Enum DamageCategory vào mảng tĩnh
        int numCategories = 1 + random.nextInt(3); // Công thức sinh số lượng danh mục ngẫu nhiên từ 1 đến tối đa 3 danh mục

        // Chạy vòng lặp nhồi phần tử cho đến khi số lượng phần tử duy nhất trong Set đạt chỉ tiêu numCategories
        while (categories.size() < numCategories) {
            // Chọn ngẫu nhiên một vị trí index và lấy Enum DamageCategory tại vị trí đó nhét vào Set
            categories.add(allCategories[random.nextInt(allCategories.length)]);
        }
        return categories; // Xuất tập hợp danh mục đã bốc thăm xong
    }

    /**
     * Bốc thăm danh mục thiệt hại cho khối Bình luận (Tỷ lệ bình luận có chứa từ khóa thiệt hại thấp hơn bài viết).
     */
    private Set<DamageCategory> generateCommentDamageCategories() {
        Set<DamageCategory> categories = new HashSet<>(); // Khởi tạo HashSet lưu trữ danh mục
        DamageCategory[] allCategories = DamageCategory.values(); // Đổ toàn bộ giá trị Enum vào mảng
        double rand = random.nextDouble(); // Sinh một số thập phân ngẫu nhiên từ 0.0 đến nhỏ hơn 1.0

        // Thuật toán chia tỷ lệ xuất hiện từ khóa thiệt hại trong bình luận:
        // Có 40% tỷ lệ bình luận hoàn toàn sạch không chứa thiệt hại (số lượng danh mục = 0)
        // Có 40% tỷ lệ bình luận chứa chính xác 1 danh mục thiệt hại (số lượng danh mục = 1)
        // Có 20% tỷ lệ bình luận chứa 2 danh mục thiệt hại (số lượng danh mục = 2)
        int numCategories = (rand < 0.4) ? 0 : (rand < 0.8) ? 1 : 2;

        // Vòng lặp nhét ngẫu nhiên các danh mục Enum vào Set cho đến khi đạt đủ chỉ tiêu numCategories ở trên
        while (categories.size() < numCategories) {
            categories.add(allCategories[random.nextInt(allCategories.length)]);
        }
        return categories; // Xuất tập hợp danh mục bình luận
    }

    /**
     * Đọc qua các danh mục thiệt hại đã chọn, tra cứu mảng từ khóa tiếng Việt tương ứng,
     * chọn ngẫu nhiên một từ và nối đuôi vào sau câu văn gốc để tạo thành văn bản thực tế.
     */
    private String enhanceContentWithDamageKeywords(String content, Set<DamageCategory> categories) {
        StringBuilder enhanced = new StringBuilder(content); // Sử dụng StringBuilder để tối ưu hóa hiệu năng ghép nối chuỗi chữ

        // Duyệt tuần tự qua từng danh mục thiệt hại có trong tập hợp Set truyền vào
        for (DamageCategory category : categories) {
            String[] keywords = DAMAGE_KEYWORDS.get(category); // Dùng tên danh mục làm chìa khóa (Key) tra cứu mảng từ khóa trong bản đồ Map
            // Gác cổng: Đảm bảo mảng từ khóa tra được tồn tại và chứa dữ liệu chữ bên trong
            if (keywords != null && keywords.length > 0) {
                String keyword = keywords[random.nextInt(keywords.length)]; // Bốc thăm ngẫu nhiên đúng 1 từ khóa tiếng Việt trong mảng
                // Nối chuỗi chữ cấu trúc phụ họa vào cuối văn bản gốc nhằm mô phỏng dữ liệu thô chuẩn xác
                enhanced.append(" Ghi nhận tình trạng ").append(keyword).append(".");
            }
        }
        return enhanced.toString(); // Trả về chuỗi văn bản tiếng Việt dài đã được tối ưu hóa tăng cường từ khóa
    }

    /**
     * Quét chuỗi văn bản của bài viết gốc xem có chứa tên thiên tai nào trong danh sách mảng tĩnh không để bóc tách ngược lại.
     */
    private String extractDisasterName(String postContent) {
        if (postContent == null || postContent.isEmpty()) return ""; // Bảo vệ an toàn nếu chuỗi văn bản đầu vào trống rỗng
        // Duyệt qua từng tên thiên tai có trong kho lưu trữ tĩnh DISASTER_NAMES
        for (String disasterName : DISASTER_NAMES) {
            // Nếu phát hiện câu văn chứa cụm từ thiên tai này (Ví dụ chứa chữ "Siêu bão Yagi")
            if (postContent.contains(disasterName)) {
                return disasterName; // Trả về ngay lập tức tên thiên tai đó để làm ngữ cảnh cho luồng bình luận
            }
        }
        return ""; // Trả về chuỗi rỗng nếu bài viết không chứa từ khóa bão lũ nào quen thuộc
    }
}