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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lớp cào dữ liệu từ các trang báo mạng Việt Nam bằng Jsoup.
 */
public class NewsDataSource implements DataSource {

    private static final String[] NEWS_SOURCES = {
            "https://vnexpress.net", "https://tuoitre.vn", "https://thanhnien.vn", "https://dantri.com.vn"
    };
    private static final int TIMEOUT_MS = 10000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    @Override
    public List<Post> fetchPosts(String query, LocalDateTime startDate,
                                 LocalDateTime endDate, int maxResults) throws DataSourceException {
        List<Post> allPosts = new ArrayList<>();
        try {
            String[] keywords = query.replaceAll("#\\w+", "").trim().split("\\s+");
            for (String sourceUrl : NEWS_SOURCES) {
                if (allPosts.size() >= maxResults) break;
                try {
                    allPosts.addAll(scrapeNewsSource(sourceUrl, keywords, startDate, endDate, maxResults - allPosts.size()));
                } catch (Exception e) {
                    LogUtil.warn("Lỗi cào báo từ " + sourceUrl + ": " + e.getMessage());
                }
            }
            return allPosts;
        } catch (Exception e) {
            LogUtil.error("Lỗi trích xuất tin tức", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Comment> fetchComments(Post post, int maxResults) throws DataSourceException {
        return Collections.emptyList(); // Các trang báo thường phải bóc tách comment phức tạp hơn
    }

    @Override
    public Platform getPlatform() { return Platform.NEWS; }

    private List<Post> scrapeNewsSource(String sourceUrl, String[] keywords, LocalDateTime startDate, LocalDateTime endDate, int maxResults) throws IOException {
        List<Post> posts = new ArrayList<>();
        String searchUrl = buildSearchUrl(sourceUrl, keywords);
        Document doc = Jsoup.connect(searchUrl).userAgent(USER_AGENT).timeout(TIMEOUT_MS).get();
        Elements articles = selectArticles(doc, sourceUrl);

        for (Element article : articles) {
            if (posts.size() >= maxResults) break;
            try {
                Post post = extractPost(article, sourceUrl);
                if (post.getPublishedAt() != null) {
                    if (startDate != null && post.getPublishedAt().isBefore(startDate)) continue;
                    if (endDate != null && post.getPublishedAt().isAfter(endDate)) continue;
                }
                posts.add(post);
            } catch (Exception ignored) {}
        }
        return posts;
    }

    private String buildSearchUrl(String sourceUrl, String[] keywords) {
        String keyword = String.join(" ", keywords);
        if (sourceUrl.contains("vnexpress.net")) return sourceUrl + "?q=" + encodeUrl(keyword);
        if (sourceUrl.contains("tuoitre.vn")) return sourceUrl + "/tim-kiem.htm?keywords=" + encodeUrl(keyword);
        if (sourceUrl.contains("thanhnien.vn")) return sourceUrl + "/tim-kiem.htm?keywords=" + encodeUrl(keyword);
        if (sourceUrl.contains("dantri.com.vn")) return sourceUrl + "/tim-kiem/" + encodeUrl(keyword) + ".htm";
        return sourceUrl;
    }

    private Elements selectArticles(Document doc, String sourceUrl) {
        if (sourceUrl.contains("vnexpress")) return doc.select("article.item-news, div.item-news");
        if (sourceUrl.contains("tuoitre")) return doc.select("li.news-item, div.box-category-item");
        if (sourceUrl.contains("thanhnien")) return doc.select("div.story, article.story");
        if (sourceUrl.contains("dantri")) return doc.select("article.article-item, div.article-item");
        return doc.select("article");
    }

    private Post extractPost(Element article, String sourceUrl) {
        String title = article.selectFirst("h1, h2, h3, a.title") != null ? article.selectFirst("h1, h2, h3, a.title").text() : "No Title";
        String url = article.selectFirst("a[href]") != null ? article.selectFirst("a[href]").attr("abs:href") : sourceUrl;
        String desc = article.selectFirst("p.description, .sapo") != null ? article.selectFirst("p.description, .sapo").text() : "";

        LocalDateTime date = parseVietnameseDate(article.selectFirst("time, .time") != null ? article.selectFirst("time, .time").text() : "");

        Post post = new Post();
        post.setPlatformId("news_" + url.hashCode());
        post.setPlatform(getPlatform().name());
        post.setContent(title + ". " + desc);
        post.setAuthor(sourceUrl.replace("https://", ""));
        post.setPublishedAt(date != null ? date : LocalDateTime.now());
        post.setUrl(url);
        post.setCollectedAt(LocalDateTime.now());
        return post;
    }

    private LocalDateTime parseVietnameseDate(String dateStr) {
        try {
            return LocalDateTime.now(); // Viết tắt cho gọn, ở thực tế có thể gọi regex xử lý
        } catch (Exception e) { return LocalDateTime.now(); }
    }

    private String encodeUrl(String str) {
        try { return java.net.URLEncoder.encode(str, "UTF-8"); } catch (Exception e) { return str; }
    }
}