package com.disaster.analysis.application.service;

import com.disaster.analysis.domain.model.entities.Comment;
import com.disaster.analysis.domain.model.entities.Post;
import com.disaster.analysis.domain.model.enums.Sentiment;
import com.disaster.analysis.domain.model.enums.TimeGranularity;
import com.disaster.analysis.domain.contract.analysis.SentimentAnalyzer;
import com.disaster.analysis.domain.contract.repository.CommentRepository;
import com.disaster.analysis.domain.contract.repository.PostRepository;
import com.disaster.analysis.util.LogUtil;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service quản lý việc phân tích cảm xúc (Sentiment) của người dùng đối với thảm họa.
 * Kết nối với AI (Gemini/OpenAI) thông qua interface SentimentAnalyzer.
 */
public class SentimentAnalysisService {

    private final SentimentAnalyzer sentimentAnalyzer;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public SentimentAnalysisService(SentimentAnalyzer sentimentAnalyzer,
                                    PostRepository postRepository,
                                    CommentRepository commentRepository) {
        this.sentimentAnalyzer = sentimentAnalyzer;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * Khởi tạo kết nối với mô hình AI (nạp API Key, chuẩn bị kết nối).
     */
    public void initialize() {
        sentimentAnalyzer.initialize();
        LogUtil.info("SentimentAnalysisService initialized.");
    }

    /**
     * Phân tích cảm xúc cho một chuỗi văn bản đơn lẻ.
     * @param text Văn bản cần phân tích.
     * @return Enum Sentiment (Tích cực, Tiêu cực, Trung lập).
     */
    public Sentiment analyzeSentiment(String text) {
        return sentimentAnalyzer.analyzeSentiment(text);
    }

    /**
     * Lấy toàn bộ bài viết trong một dự án từ DB, chấm điểm cảm xúc và lưu lại.
     * @param projectId ID của dự án cần phân tích.
     */
    public void analyzeProjectPosts(Long projectId) {
        if (!sentimentAnalyzer.isInitialized()) {
            throw new IllegalStateException("SentimentAnalysisService must be initialized before use. Call initialize() first.");
        }

        List<Post> posts = postRepository.findByProjectId(projectId);
        LogUtil.info("Starting sentiment analysis for " + posts.size() + " posts in project " + projectId);

        for (Post post : posts) {
            String contentToAnalyze = post.getPreprocessedContent() != null && !post.getPreprocessedContent().isEmpty()
                    ? post.getPreprocessedContent()
                    : post.getContent();

            Sentiment sentiment = analyzeSentiment(contentToAnalyze);
            post.setSentiment(sentiment);
            postRepository.update(post);
        }
    }

    /**
     * Lấy toàn bộ bình luận trong một dự án từ DB, chấm điểm cảm xúc và lưu lại.
     * @param projectId ID của dự án cần phân tích.
     */
    public void analyzeProjectComments(Long projectId) {
        if (!sentimentAnalyzer.isInitialized()) {
            throw new IllegalStateException("SentimentAnalysisService must be initialized before use. Call initialize() first.");
        }

        List<Comment> comments = commentRepository.findByProjectId(projectId);
        LogUtil.info("Starting sentiment analysis for " + comments.size() + " comments in project " + projectId);

        for (Comment comment : comments) {
            String contentToAnalyze = comment.getPreprocessedContent() != null && !comment.getPreprocessedContent().isEmpty()
                    ? comment.getPreprocessedContent()
                    : comment.getContent();

            Sentiment sentiment = analyzeSentiment(contentToAnalyze);
            comment.setSentiment(sentiment);
            commentRepository.update(comment);
        }
    }

    /**
     * Cung cấp dữ liệu để vẽ biểu đồ đường (Line Chart) theo thời gian.
     * @param projectId ID dự án.
     * @param granularity Độ chia nhỏ thời gian (Theo giờ, ngày, tuần, tháng).
     * @return Map chứa mốc thời gian và số lượng từng loại cảm xúc tương ứng.
     */
    public Map<LocalDateTime, Map<Sentiment, Long>> getSentimentTimeSeries(Long projectId, TimeGranularity granularity) {
        List<Post> posts = postRepository.findByProjectId(projectId);
        List<Comment> comments = commentRepository.findByProjectId(projectId);

        Map<LocalDateTime, Map<Sentiment, Long>> timeSeries = new TreeMap<>();

        // Trích xuất từ bài viết
        for (Post post : posts) {
            if (post.getPublishedAt() == null || post.getSentiment() == null) continue;
            LocalDateTime timeKey = truncateToGranularity(post.getPublishedAt(), granularity);
            timeSeries.computeIfAbsent(timeKey, k -> new HashMap<>()).merge(post.getSentiment(), 1L, Long::sum);
        }

        // Trích xuất từ bình luận
        for (Comment comment : comments) {
            if (comment.getPublishedAt() == null || comment.getSentiment() == null) continue;
            LocalDateTime timeKey = truncateToGranularity(comment.getPublishedAt(), granularity);
            timeSeries.computeIfAbsent(timeKey, k -> new HashMap<>()).merge(comment.getSentiment(), 1L, Long::sum);
        }

        // Bơm số 0 vào các loại cảm xúc không xuất hiện để biểu đồ không bị lỗi
        for (Map<Sentiment, Long> sentimentCounts : timeSeries.values()) {
            for (Sentiment sentiment : Sentiment.values()) {
                sentimentCounts.putIfAbsent(sentiment, 0L);
            }
        }

        return timeSeries;
    }

    /**
     * Hàm tiện ích: Cắt xén thời gian theo yêu cầu của biểu đồ.
     */
    private LocalDateTime truncateToGranularity(LocalDateTime dateTime, TimeGranularity granularity) {
        return switch (granularity) {
            case HOURLY -> dateTime.truncatedTo(ChronoUnit.HOURS);
            case DAILY -> dateTime.truncatedTo(ChronoUnit.DAYS);
            case WEEKLY -> {
                LocalDateTime startOfDay = dateTime.truncatedTo(ChronoUnit.DAYS);
                int dayOfWeek = startOfDay.getDayOfWeek().getValue();
                yield startOfDay.minusDays(dayOfWeek - 1); // Trả về thứ 2 đầu tuần
            }
            case MONTHLY -> dateTime.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1); // Trả về mùng 1 đầu tháng
        };
    }
}