package com.disaster.analysis.infrastructure.social;

import com.disaster.analysis.domain.contract.social.DataSource;
import com.disaster.analysis.domain.exception.DataSourceException;
import com.disaster.analysis.domain.model.Comment;
import com.disaster.analysis.domain.model.Post;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Platform;
import com.disaster.analysis.domain.model.enums.Sentiment;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Bộ sinh dữ liệu giả lập (Mock DataSource).
 * Tự động tạo ra các bài viết và bình luận Tiếng Việt giống hệt mạng xã hội thực tế
 * để phục vụ quá trình test UI mà không cần gọi API thật.
 */
public class MockDataSource implements DataSource {

    private final Platform platform;
    private final Random random;

    // Danh sách tên thảm họa phổ biến tại Việt Nam
    private static final String[] DISASTER_NAMES = {
            "Siêu bão Yagi", "Bão số 3", "Hoàn lưu bão Yagi",
            "Lũ lụt miền Bắc", "Sạt lở đất tại Lào Cai", "Lũ quét Yên Bái",
            "Bão Noru", "Mưa lũ miền Trung"
    };

    // Mẫu câu Tích cực (Lạc quan, cứu trợ, tương thân tương ái)
    private static final String[] POSITIVE_TEMPLATES = {
            "Công tác cứu trợ cho %s đang diễn ra rất nhanh chóng! Cả nước đang hướng về bà con.",
            "Thật biết ơn các anh bộ đội và tình nguyện viên đã không quản ngày đêm giúp đỡ nạn nhân %s.",
            "Tin vui: Hàng chục chuyến xe chở nhu yếu phẩm đã tiếp cận được vùng rốn lũ %s.",
            "Tuyệt vời tinh thần tương thân tương ái của người Việt Nam trong %s! Cố lên đồng bào ơi.",
            "Nước đã bắt đầu rút sau %s, bà con đang dọn dẹp nhà cửa. Mong mọi thứ sớm bình yên.",
            "Đã cứu thành công nhiều hộ gia đình bị mắc kẹt do %s. Thật sự nể phục lực lượng cứu hộ!",
            "Chính quyền địa phương phản ứng rất kịp thời trong đợt %s này. Mọi người đã được di dời đến nơi an toàn."
    };

    // Mẫu câu Tiêu cực (Thiệt hại, mất mát, hoảng sợ)
    private static final String[] NEGATIVE_TEMPLATES = {
            "Hậu quả từ %s quá tàn khốc. Hàng ngàn ngôi nhà bị ngập tới nóc, người dân bơ vơ.",
            "Đau xót quá, %s đã cướp đi sinh mạng của bao nhiêu người. Khung cảnh thật tang thương.",
            "Hiện tại vùng này đang bị cô lập hoàn toàn do %s, mất điện, mất sóng, cạn kiệt thức ăn.",
            "Nước lên nhanh quá trở tay không kịp! %s phá hủy toàn bộ tài sản và hoa màu của bà con rồi.",
            "Kêu cứu: Cần lắm ca nô và áo phao! Khu vực tôi đang bị ngập sâu sau %s.",
            "Ám ảnh thực sự. Cả một mảng đồi sạt lở vùi lấp bao nhiêu nhà dân trong đợt %s vừa rồi.",
            "Đường sá nát bét, sạt lở khắp nơi do %s. Thiệt hại kinh tế đợt này chắc chắn rất khủng khiếp."
    };

    // Mẫu câu Trung lập (Cập nhật tin tức, số liệu)
    private static final String[] NEUTRAL_TEMPLATES = {
            "Cập nhật tình hình %s: Cơ quan khí tượng dự báo mưa lớn sẽ còn tiếp diễn trong 2 ngày tới.",
            "Báo cáo nhanh: Thống kê ban đầu cho thấy %s đã làm ảnh hưởng đến hơn 50.000 hộ dân.",
            "Chính quyền đang họp khẩn để đưa ra phương án khắc phục hậu quả của %s.",
            "Tổng hợp danh sách các tuyến đường đang bị phong tỏa do ảnh hưởng của %s để mọi người né tránh.",
            "Thông tin tài khoản chính thức của Mặt trận Tổ quốc để quyên góp cho đồng bào chịu ảnh hưởng bởi %s.",
            "Họp báo chính phủ công bố gói ngân sách hỗ trợ tái thiết cơ sở hạ tầng sau %s.",
            "Nhiều trường học thông báo cho học sinh nghỉ học để đảm bảo an toàn trong đợt %s."
    };

    // Từ khóa thiệt hại để chèn thêm vào câu cho tự nhiên
    private static final Map<DamageCategory, String[]> DAMAGE_KEYWORDS = new HashMap<>();
    static {
        DAMAGE_KEYWORDS.put(DamageCategory.PEOPLE_AFFECTED, new String[]{
                "người bị thương", "nạn nhân", "người mất tích", "bà con mắc kẹt", "người tử vong"
        });
        DAMAGE_KEYWORDS.put(DamageCategory.ECONOMIC_DISRUPTION, new String[]{
                "doanh nghiệp điêu đứng", "hàng quán đóng cửa", "chuỗi cung ứng đứt gãy", "thiệt hại kinh tế nặng"
        });
        DAMAGE_KEYWORDS.put(DamageCategory.BUILDING_DAMAGE, new String[]{
                "nhà cửa đổ sập", "tốc mái", "trường học tốc mái", "nhà bị lũ cuốn trôi", "công trình hư hỏng"
        });
        DAMAGE_KEYWORDS.put(DamageCategory.PERSONAL_PROPERTY_LOSS, new String[]{
                "trôi hết xe cộ", "tài sản mất sạch", "đồ đạc ngập trong nước", "mất trắng lợn gà"
        });
        DAMAGE_KEYWORDS.put(DamageCategory.INFRASTRUCTURE_DAMAGE, new String[]{
                "sập cầu", "đứt cáp quang", "mất điện diện rộng", "sạt lở quốc lộ", "mất nước sạch"
        });
        DAMAGE_KEYWORDS.put(DamageCategory.OTHER, new String[]{
                "mất trắng mùa màng", "cây xanh gãy đổ la liệt", "môi trường ô nhiễm", "ngập úng lúa"
        });
    }

    // Tác giả Bài viết giả lập
    private static final String[] AUTHORS = {
            "Phóng Viên Thường Trú", "Người Dân Địa Phương", "Đội Cứu Hộ", "Ban Chỉ Đạo Phòng Chống Thiên Tai",
            "Trang Tin 24h", "Hội Chữ Thập Đỏ VN", "Tình Nguyện Viên", "Mặt Trận Tổ Quốc", "Tin Tức VTV"
    };

    // Mẫu Bình luận (Bên dưới bài viết)
    private static final String[] POSITIVE_COMMENT_TEMPLATES = {
            "Cảm ơn bạn đã cập nhật thông tin. Cầu mong mọi người bình an!",
            "Tuyệt vời quá, cảm ơn các mạnh thường quân.",
            "Ấm lòng thực sự. Miền Nam luôn hướng về miền Bắc thân yêu!",
            "Mình có một thuyền máy nhỏ, liên hệ ai để tham gia cứu hộ đây ạ?",
            "Đã chuyển khoản ủng hộ quỹ. Mong bà con sớm vượt qua khó khăn."
    };

    private static final String[] NEGATIVE_COMMENT_TEMPLATES = {
            "Nhìn video mà xót xa quá rơi nước mắt luôn.",
            "Trời ơi sập hết nhà cửa thế kia thì sống sao đây trời?",
            "Chỗ nhà ngoại em đang ngập đến nóc rồi, gọi cứu hộ mà chưa thấy tới. Lo quá!",
            "Thiên nhiên nổi giận thật đáng sợ. Mất mát này quá lớn.",
            "Cảnh tượng kinh hoàng thực sự, năm nay thiên tai khắc nghiệt quá."
    };

    private static final String[] NEUTRAL_COMMENT_TEMPLATES = {
            "Cho mình xin thông tin liên hệ của nhóm cứu trợ khu vực này với ạ.",
            "Hiện tại quốc lộ có đi được không mọi người?",
            "Có ai biết tình hình ở huyện Trấn Yên sao rồi không ạ?",
            "Đã share bài viết để nhiều người biết tới thông tin này.",
            "Chỗ mình đang cúp điện, ai có số tổng đài điện lực cho mình xin."
    };

    private static final String[] COMMENT_AUTHORS = {
            "Nguyen Van A", "Tran Thi B", "Nguoi_Qua_Duong", "Hoang Lan",
            "Thanh Tung", "Lan Anh 99", "Phuot_Thu", "Hanh_Phuc_Quanh_Ta",
            "Dung_Xe_Khach", "Thao_My_Store"
    };

    public MockDataSource(Platform platform) {
        this.platform = platform;
        this.random = new Random();
    }

    public MockDataSource() {
        this(Platform.YOUTUBE);
    }

    @Override
    public List<Post> fetchPosts(String query, LocalDateTime startDate,
                                 LocalDateTime endDate, int maxResults) throws DataSourceException {

        if (query == null || query.trim().isEmpty()) {
            throw new DataSourceException(platform, "Từ khóa tìm kiếm không được để trống.");
        }
        if (startDate == null || endDate == null) {
            throw new DataSourceException(platform, "Ngày bắt đầu và kết thúc không được để trống.");
        }
        if (startDate.isAfter(endDate)) {
            throw new DataSourceException(platform, "Ngày bắt đầu không được lớn hơn ngày kết thúc.");
        }

        List<Post> posts = new ArrayList<>();
        // Sinh ra số lượng bài viết từ 50% đến 100% của maxResults
        int numPosts = maxResults / 2 + random.nextInt(maxResults / 2 + 1);

        for (int i = 0; i < numPosts; i++) {
            posts.add(generateMockPost(query, startDate, endDate));
        }
        return posts;
    }

    @Override
    public List<Comment> fetchComments(Post post, int maxResults) throws DataSourceException {
        List<Comment> comments = new ArrayList<>();
        // Lượng comment ngẫu nhiên từ 30% đến 100% của maxResults
        int numComments = (maxResults * 3) / 10 + random.nextInt((maxResults * 7) / 10 + 1);
        String disasterContext = extractDisasterName(post.getContent());

        for (int i = 0; i < numComments; i++) {
            comments.add(generateMockComment(post, disasterContext));
        }
        return comments;
    }

    @Override
    public Platform getPlatform() {
        return platform;
    }

    // KHỐI SINH BÀI VIẾT (POST) GIẢ LẬP
    private Post generateMockPost(String query, LocalDateTime startDate, LocalDateTime endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        long randomDays = daysBetween > 0 ? ThreadLocalRandom.current().nextLong(daysBetween + 1) : 0;
        LocalDateTime publishedAt = startDate.plusDays(randomDays)
                .plusHours(random.nextInt(24))
                .plusMinutes(random.nextInt(60));

        Sentiment sentiment = Sentiment.values()[random.nextInt(Sentiment.values().length)];
        Set<DamageCategory> categories = generateDamageCategories();

        String content = generateContent(sentiment);
        String enhancedContent = enhanceContentWithDamageKeywords(content, categories);

        String platformId = platform.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        String author = AUTHORS[random.nextInt(AUTHORS.length)];
        String url = "https://" + platform.name().toLowerCase() + ".com/post/" + platformId;

        // Ép kiểu Set<Enum> thành chuỗi String để lưu Database chuẩn xác
        String damageStr = categories.stream()
                .map(DamageCategory::name)
                .collect(Collectors.joining(","));

        Post post = new Post();
        post.setPlatformId(platformId);
        post.setPlatform(platform.name()); // CHUẨN: Dùng String thay vì Enum
        post.setContent(enhancedContent);
        post.setAuthor(author);
        post.setPublishedAt(publishedAt);
        post.setUrl(url);
        post.setDamageCategories(damageStr); // CHUẨN: Dùng String
        post.setSentiment(sentiment);
        post.setCollectedAt(LocalDateTime.now());

        return post;
    }

    // KHỐI SINH BÌNH LUẬN (COMMENT) GIẢ LẬP
    private Comment generateMockComment(Post post, String disasterContext) {
        long hoursAfterPost = random.nextInt(24 * 30);
        LocalDateTime commentTime = post.getPublishedAt().plusHours(hoursAfterPost);

        Sentiment sentiment = Sentiment.values()[random.nextInt(Sentiment.values().length)];
        Set<DamageCategory> categories = generateCommentDamageCategories();

        String content = generateCommentContent(sentiment, disasterContext);
        String enhancedContent = enhanceContentWithDamageKeywords(content, categories);

        String platformId = platform.name().toLowerCase() + "_cmt_" + UUID.randomUUID().toString().substring(0, 8);
        String author = COMMENT_AUTHORS[random.nextInt(COMMENT_AUTHORS.length)];

        // Ép kiểu Set<Enum> thành chuỗi String để lưu Database chuẩn xác
        String damageStr = categories.stream()
                .map(DamageCategory::name)
                .collect(Collectors.joining(","));

        Comment comment = new Comment();
        comment.setPostId(post.getId());
        comment.setProjectId(post.getProjectId());
        comment.setPlatformId(platformId);
        comment.setPlatform(platform.name()); // CHUẨN: Dùng String
        comment.setContent(enhancedContent);
        comment.setAuthor(author);
        comment.setPublishedAt(commentTime);
        comment.setDamageCategories(damageStr); // CHUẨN: Dùng String
        comment.setSentiment(sentiment);
        comment.setCollectedAt(LocalDateTime.now());

        return comment;
    }

    // CÁC HÀM TIỆN ÍCH TRỘN VĂN BẢN
    private String generateContent(Sentiment sentiment) {
        String[] templates = switch (sentiment) {
            case POSITIVE -> POSITIVE_TEMPLATES;
            case NEGATIVE -> NEGATIVE_TEMPLATES;
            default -> NEUTRAL_TEMPLATES;
        };
        String template = templates[random.nextInt(templates.length)];
        String disasterName = DISASTER_NAMES[random.nextInt(DISASTER_NAMES.length)];
        return String.format(template, disasterName);
    }

    private String generateCommentContent(Sentiment sentiment, String disasterContext) {
        String[] templates = switch (sentiment) {
            case POSITIVE -> POSITIVE_COMMENT_TEMPLATES;
            case NEGATIVE -> NEGATIVE_COMMENT_TEMPLATES;
            default -> NEUTRAL_COMMENT_TEMPLATES;
        };
        String template = templates[random.nextInt(templates.length)];
        return template;
    }

    private Set<DamageCategory> generateDamageCategories() {
        Set<DamageCategory> categories = new HashSet<>();
        DamageCategory[] allCategories = DamageCategory.values();
        int numCategories = 1 + random.nextInt(3);
        while (categories.size() < numCategories) {
            categories.add(allCategories[random.nextInt(allCategories.length)]);
        }
        return categories;
    }

    private Set<DamageCategory> generateCommentDamageCategories() {
        Set<DamageCategory> categories = new HashSet<>();
        DamageCategory[] allCategories = DamageCategory.values();
        double rand = random.nextDouble();
        int numCategories = (rand < 0.4) ? 0 : (rand < 0.8) ? 1 : 2;
        while (categories.size() < numCategories) {
            categories.add(allCategories[random.nextInt(allCategories.length)]);
        }
        return categories;
    }

    private String enhanceContentWithDamageKeywords(String content, Set<DamageCategory> categories) {
        StringBuilder enhanced = new StringBuilder(content);
        for (DamageCategory category : categories) {
            String[] keywords = DAMAGE_KEYWORDS.get(category);
            if (keywords != null && keywords.length > 0) {
                String keyword = keywords[random.nextInt(keywords.length)];
                enhanced.append(" Ghi nhận tình trạng ").append(keyword).append(".");
            }
        }
        return enhanced.toString();
    }

    private String extractDisasterName(String postContent) {
        if (postContent == null || postContent.isEmpty()) return "";
        for (String disasterName : DISASTER_NAMES) {
            if (postContent.contains(disasterName)) return disasterName;
        }
        return "";
    }
}