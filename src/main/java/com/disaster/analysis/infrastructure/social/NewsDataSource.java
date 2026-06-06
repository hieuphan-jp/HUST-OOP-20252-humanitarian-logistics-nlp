package com.disaster.analysis.infrastructure.social;

import com.disaster.analysis.domain.exception.DataSourceException;
import com.disaster.analysis.domain.model.Comment;
import com.disaster.analysis.domain.model.Post;
import com.disaster.analysis.domain.model.enums.Platform;
import com.disaster.analysis.domain.contract.social.DataSource;
import com.disaster.analysis.util.LogUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lớp chịu trách nhiệm cào dữ liệu (scraping) từ các trang báo điện tử Việt Nam.
 * Đã NÂNG CẤP LÊN CHIẾN THUẬT CÀO 2 BƯỚC (Two-step Scraping) để lấy nội dung chi tiết.
 */
public class NewsDataSource implements DataSource {

    private static final int TIMEOUT_MS = 15000; // Tăng thời gian chờ lên 15s vì phải vào từng trang chi tiết
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // Danh sách cấu hình động cho 4 tờ báo lớn
    private static final List<SourceConfig> NEWS_SOURCES = new ArrayList<>();

    static {
        // Cấu hình VnExpress (Đã dùng URL tìm kiếm chuẩn)
        NEWS_SOURCES.add(new SourceConfig(
                "VnExpress", "https://vnexpress.net", "https://timkiem.vnexpress.net/?q=%s",
                "h3.title-news a, .item-news h3 a", // Bước 1: Selector lấy Link bài viết
                "h1.title-detail, h1.title", // Bước 2: Selector lấy Tiêu đề chi tiết
                "p.description, .sidebar-1 p.description", // Bước 2: Selector lấy Sapo
                "article.fck_detail p, .fck_detail p", // Bước 2: Selector lấy Nội dung bài viết
                "span.date, .date" // Bước 2: Selector lấy Ngày đăng
        ));

        // Cấu hình Tuổi Trẻ
        NEWS_SOURCES.add(new SourceConfig(
                "Tuổi Trẻ", "https://tuoitre.vn", "https://tuoitre.vn/tim-kiem.htm?keywords=%s",
                ".news-item .title-news a, .box-category-item .title-name a",
                ".detail-title, .article-title, h1.article-title",
                ".detail-sapo",
                ".detail-content p, #main-detail-body p",
                ".detail-time, .article-time"
        ));

        // Cấu hình Thanh Niên
        NEWS_SOURCES.add(new SourceConfig(
                "Thanh Niên", "https://thanhnien.vn", "https://thanhnien.vn/tim-kiem.htm?keywords=%s",
                ".story__title a, .box-category-item a.box-category-link-title",
                ".detail-title, h1.detail-title",
                ".detail-sapo",
                ".detail-cmain p",
                ".detail-time"
        ));

        // Cấu hình Dân Trí
        NEWS_SOURCES.add(new SourceConfig(
                "Dân Trí", "https://dantri.com.vn", "https://dantri.com.vn/tim-kiem/%s.htm",
                ".article-title a, h3.article-title a",
                ".title-page, h1.title-page",
                ".singular-sapo",
                ".singular-content p",
                ".author-time"
        ));
    }

    public NewsDataSource() {
        LogUtil.info("NewsDataSource initialized with Two-Step Scraping Strategy for 4 sources.");
    }

    @Override
    public List<Post> fetchPosts(String query, LocalDateTime startDate,
                                 LocalDateTime endDate, int maxResults) throws DataSourceException {

        LogUtil.info("Fetching detailed news articles for query: '" + query + "'");
        List<Post> allPosts = new ArrayList<>();
        Set<String> processedUrls = new HashSet<>(); // Bộ nhớ đệm chống cào trùng lặp link

        try {
            // Làm sạch từ khóa
            String cleanQuery = query.replaceAll("#\\w+", "").trim();
            String encodedQuery = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8.toString());

            // Duyệt qua 4 đầu báo
            for (SourceConfig config : NEWS_SOURCES) {
                if (allPosts.size() >= maxResults) break;

                try {
                    LogUtil.info("--------------------------------------------------");
                    LogUtil.info("Source [" + config.name + "]: Starting extraction...");

                    String searchUrl = String.format(config.searchUrlTemplate, encodedQuery);
                    int remainingQuota = maxResults - allPosts.size();

                    // GỌI HÀM CÀO 2 BƯỚC
                    List<Post> posts = scrapeTwoSteps(config, searchUrl, startDate, endDate, remainingQuota, processedUrls, keywordsFromQuery(cleanQuery));
                    allPosts.addAll(posts);

                    LogUtil.info("Source [" + config.name + "]: Finished. Extracted " + posts.size() + " full articles.");
                } catch (Exception e) {
                    LogUtil.warn("Source [" + config.name + "]: Failed - " + e.getMessage());
                }
            }

            LogUtil.info("News scraping completed. Total full articles retrieved: " + allPosts.size());
            return allPosts;

        } catch (Exception e) {
            LogUtil.error("Critical error in NewsDataSource", e);
            throw new DataSourceException(getPlatform(), "System error while scraping news: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Comment> fetchComments(Post post, int maxResults) throws DataSourceException {
        return Collections.emptyList(); // Báo điện tử không cào bình luận
    }

    @Override
    public Platform getPlatform() {
        return Platform.NEWS;
    }

    // LÕI CÀO 2 BƯỚC (TWO-STEP SCRAPING)
    private List<Post> scrapeTwoSteps(SourceConfig config, String searchUrl,
                                      LocalDateTime startDate, LocalDateTime endDate,
                                      int maxResults, Set<String> processedUrls, String[] keywords) throws IOException {
        List<Post> posts = new ArrayList<>();

        // BƯỚC 1: Truy cập trang tìm kiếm chỉ để lấy danh sách URL
        Document searchPage = Jsoup.connect(searchUrl)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();

        Elements linkElements = searchPage.select(config.linkSelector);

        if (linkElements.isEmpty()) {
            LogUtil.warn("   -> Warning: No article links found on the search page.");
            return posts;
        }

        // BƯỚC 2: Vào TỪNG ĐƯỜNG LINK chi tiết để cào nội dung sâu
        for (Element linkEl : linkElements) {
            if (posts.size() >= maxResults) break;

            String articleUrl = linkEl.attr("abs:href");
            if (articleUrl.isEmpty() || processedUrls.contains(articleUrl)) continue;

            processedUrls.add(articleUrl);
            LogUtil.info("   -> Fetching detail: " + articleUrl);

            try {
                // Tải trang chi tiết bài báo
                Document detailPage = Jsoup.connect(articleUrl)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .get();

                // 2.1 - Bóc tách ngày tháng trước để lọc
                LocalDateTime publishedAt = extractDateFromDetail(detailPage, config.dateSelector);

                // Nếu báo ẩn ngày, hoặc ngày nằm ngoài vùng phủ sóng -> Bỏ qua ngay lập tức để tiết kiệm RAM
                if (publishedAt == null) {
                    LogUtil.warn("      ✗ Skipped: Failed to extract publication date.");
                    continue;
                }
                if (startDate != null && publishedAt.isBefore(startDate)) {
                    LogUtil.warn("      ✗ Skipped: Article is older than the start date.");
                    continue;
                }
                if (endDate != null && publishedAt.isAfter(endDate)) {
                    LogUtil.warn("      ✗ Skipped: Article is newer than the end date.");
                    continue;
                }

                // 2.2 - Lọt qua bộ lọc ngày -> Bóc tách toàn bộ Nội dung
                String title = extractText(detailPage, config.titleSelector, "Untitled Article");
                String sapo = extractText(detailPage, config.sapoSelector, "");

                // Lấy nội dung chi tiết: Nối tất cả các đoạn văn <p> lại với nhau
                StringBuilder bodyText = new StringBuilder();
                Elements paragraphs = detailPage.select(config.contentSelector);
                for (Element p : paragraphs) {
                    bodyText.append(p.text()).append(" ");
                }

                String fullContent = title + ". " + sapo + " " + bodyText.toString().trim();

                // Lọc từ khóa lần cuối trên nội dung chi tiết (Đảm bảo độ chính xác)
                if (!containsKeywords(fullContent, keywords)) {
                    LogUtil.warn("      ✗ Skipped: Detailed content does not contain the required keywords.");
                    continue;
                }

                // Gói gọn vào Post
                String platformId = "news_" + Math.abs(articleUrl.hashCode());
                Post post = new Post();
                post.setPlatformId(platformId);
                post.setPlatform(getPlatform().name());
                post.setContent(fullContent);
                post.setAuthor(config.name);
                post.setPublishedAt(publishedAt);
                post.setUrl(articleUrl);
                post.setCollectedAt(LocalDateTime.now());

                posts.add(post);
                LogUtil.info("      ✓ Success!");

                // Giả lập thời gian nghỉ (Delay) để chống bị Block như code VnExpressCollector của bạn
                Thread.sleep(1000);

            } catch (Exception e) {
                LogUtil.warn("      ✗ Error loading article details: " + e.getMessage());
            }
        }

        return posts;
    }

    // CÁC HÀM TIỆN ÍCH TRÍCH XUẤT VÀ KIỂM TRA
    private String extractText(Document doc, String selector, String defaultValue) {
        Element target = doc.selectFirst(selector);
        return target != null ? target.text().trim() : defaultValue;
    }

    /**
     * Tích hợp Regex bóc tách ngày siêu mạnh từ file VnExpressCollector của bạn.
     */
    private LocalDateTime extractDateFromDetail(Document doc, String dateSelector) {
        Element dateElement = doc.selectFirst(dateSelector);
        if (dateElement == null) return null;

        String rawTimeStr = dateElement.text().trim();
        if (rawTimeStr.isEmpty()) return null;

        try {
            // Regex tìm chính xác định dạng "ngày/tháng/năm" và "giờ:phút" bất kể văn bản xung quanh
            Matcher m = Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4}).*?(\\d{1,2}:\\d{2})").matcher(rawTimeStr);
            if (m.find()) {
                String cleanTimeStr = m.group(1) + " " + m.group(2);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy H:m");
                return LocalDateTime.parse(cleanTimeStr, formatter);
            }

            // Regex dự phòng trường hợp thứ 2: "ngày-tháng-năm"
            Matcher m2 = Pattern.compile("(\\d{1,2}-\\d{1,2}-\\d{4}).*?(\\d{1,2}:\\d{2})").matcher(rawTimeStr);
            if (m2.find()) {
                String cleanTimeStr = m2.group(1) + " " + m2.group(2);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy H:m");
                return LocalDateTime.parse(cleanTimeStr, formatter);
            }

        } catch (Exception e) {
            LogUtil.warn("Regex parsing error for date: " + rawTimeStr);
        }
        return null;
    }

    private String[] keywordsFromQuery(String query) {
        if (query == null || query.isEmpty()) return new String[0];
        return query.toLowerCase().split("\\s+");
    }

    private boolean containsKeywords(String content, String[] keywords) {
        if (content == null || keywords == null || keywords.length == 0) return true;
        String lowerContent = content.toLowerCase();
        for (String keyword : keywords) {
            if (lowerContent.contains(keyword)) return true;
        }
        return false;
    }

    // LỚP CẤU HÌNH ĐỘNG (INNER CLASS)
    private static class SourceConfig {
        final String name;
        final String baseUrl;
        final String searchUrlTemplate;
        final String linkSelector;      // Dùng cho Bước 1
        final String titleSelector;     // Dùng cho Bước 2
        final String sapoSelector;      // Dùng cho Bước 2
        final String contentSelector;   // Dùng cho Bước 2
        final String dateSelector;      // Dùng cho Bước 2

        SourceConfig(String name, String baseUrl, String searchUrlTemplate,
                     String linkSelector, String titleSelector,
                     String sapoSelector, String contentSelector, String dateSelector) {
            this.name = name;
            this.baseUrl = baseUrl;
            this.searchUrlTemplate = searchUrlTemplate;
            this.linkSelector = linkSelector;
            this.titleSelector = titleSelector;
            this.sapoSelector = sapoSelector;
            this.contentSelector = contentSelector;
            this.dateSelector = dateSelector;
        }
    }
}