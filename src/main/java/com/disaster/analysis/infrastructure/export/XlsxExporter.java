package com.disaster.analysis.infrastructure.export;

import com.disaster.analysis.domain.model.entities.Comment;
import com.disaster.analysis.domain.model.entities.Post;
import com.disaster.analysis.domain.model.entities.Project;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Sentiment;
import com.disaster.analysis.domain.contract.export.Exporter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Lớp thực thi hợp đồng Exporter.
 * Chịu trách nhiệm xuất toàn bộ dữ liệu dự án (Thống kê, Bài viết, Bình luận) ra file Excel (.xlsx).
 * Sử dụng thư viện Apache POI.
 */
public class XlsxExporter implements Exporter {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    // Tên các cột cho Sheet Bài viết (Posts)
    private static final String[] POSTS_COLUMNS = {
            "Post ID", "Platform ID", "Platform", "Content", "Author",
            "Published At", "URL", "Sentiment", "Damage Categories",
            "Preprocessed Content", "Collected At"
    };

    // Tên các cột cho Sheet Bình luận (Comments)
    private static final String[] COMMENTS_COLUMNS = {
            "Comment ID", "Post ID", "Platform ID", "Platform", "Content",
            "Author", "Published At", "Sentiment", "Damage Categories",
            "Preprocessed Content", "Collected At"
    };

    @Override
    public void export(Project project, List<Post> posts, List<Comment> comments, Path outputPath) throws IOException {
        try (Workbook workbook = createWorkbook(project, posts, comments);
             FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            workbook.write(fos);
        } catch (IOException e) {
            throw new IOException("Lỗi khi ghi dữ liệu ra file Excel: " + e.getMessage(), e);
        }
    }

    private Workbook createWorkbook(Project project, List<Post> posts, List<Comment> comments) {
        Workbook workbook = new XSSFWorkbook();

        createSummarySheet(workbook, project, posts, comments);
        createPostsSheet(workbook, posts);
        createCommentsSheet(workbook, comments);

        return workbook;
    }

    /**
     * Chuyển đổi chuỗi thiệt hại lưu trong DB (VD: "BUILDING_DAMAGE,OTHER")
     * thành chuỗi hiển thị đẹp trên Excel (VD: "Thiệt hại công trình; Khác").
     */
    private String formatDamageCategories(String categoriesStr) {
        if (categoriesStr == null || categoriesStr.trim().isEmpty()) {
            return "";
        }
        return Arrays.stream(categoriesStr.split(","))
                .map(String::trim)
                .map(cat -> {
                    try {
                        return DamageCategory.valueOf(cat).getDisplayName();
                    } catch (Exception e) {
                        return cat;
                    }
                })
                .collect(Collectors.joining("; "));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_FORMATTER);
    }

    // ==========================================
    // KHỐI ĐỊNH DẠNG Ô (STYLES)
    // ==========================================

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return headerStyle;
    }

    private CellStyle createContentStyle(Workbook workbook) {
        CellStyle contentStyle = workbook.createCellStyle();
        contentStyle.setWrapText(true);
        contentStyle.setVerticalAlignment(VerticalAlignment.TOP);
        return contentStyle;
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void freezeHeaderRow(Sheet sheet) {
        sheet.createFreezePane(0, 1);
    }

    // ==========================================
    // KHỐI VẼ SHEET (TẠO TRANG TÍNH)
    // ==========================================

    private void createSummarySheet(Workbook workbook, Project project, List<Post> posts, List<Comment> comments) {
        Sheet sheet = workbook.createSheet("Summary");
        int rowNum = 0;

        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 14);
        boldStyle.setFont(boldFont);

        // PHẦN 1: THÔNG TIN DỰ ÁN
        Row row = sheet.createRow(rowNum++);
        Cell cell = row.createCell(0);
        cell.setCellValue("PROJECT INFORMATION");
        cell.setCellStyle(boldStyle);

        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Project Name:");
        row.createCell(1).setCellValue(project.getName() != null ? project.getName() : "");

        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Disaster Name:");
        row.createCell(1).setCellValue(project.getDisasterName() != null ? project.getDisasterName() : "");

        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Keywords:");
        row.createCell(1).setCellValue(project.getKeywords() != null ? project.getKeywords() : "");

        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Hashtags:");
        row.createCell(1).setCellValue(project.getHashtags() != null ? project.getHashtags() : "");

        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Date Range:");
        String dateRange = formatDateTime(project.getStartDate()) + " to " + formatDateTime(project.getEndDate());
        row.createCell(1).setCellValue(dateRange);

        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Platforms:");
        row.createCell(1).setCellValue(project.getPlatforms() != null ? project.getPlatforms() : "");

        rowNum++;

        // PHẦN 2: THỐNG KÊ SỐ LƯỢNG DỮ LIỆU
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("DATA STATISTICS");
        cell.setCellStyle(boldStyle);

        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Total Posts:");
        row.createCell(1).setCellValue(posts.size());

        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Total Comments:");
        row.createCell(1).setCellValue(comments.size());

        rowNum++;

        // PHẦN 3: PHÂN BỐ CẢM XÚC (SENTIMENT)
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("SENTIMENT DISTRIBUTION");
        cell.setCellStyle(boldStyle);

        long positiveCount = posts.stream().filter(p -> p.getSentiment() == Sentiment.POSITIVE).count() +
                comments.stream().filter(c -> c.getSentiment() == Sentiment.POSITIVE).count();
        long neutralCount = posts.stream().filter(p -> p.getSentiment() == Sentiment.NEUTRAL).count() +
                comments.stream().filter(c -> c.getSentiment() == Sentiment.NEUTRAL).count();
        long negativeCount = posts.stream().filter(p -> p.getSentiment() == Sentiment.NEGATIVE).count() +
                comments.stream().filter(c -> c.getSentiment() == Sentiment.NEGATIVE).count();
        long totalWithSentiment = positiveCount + neutralCount + negativeCount;

        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Positive:");
        row.createCell(1).setCellValue(positiveCount + " (" + (totalWithSentiment > 0 ? String.format("%.1f", (positiveCount * 100.0 / totalWithSentiment)) : "0.0") + "%)");

        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Neutral:");
        row.createCell(1).setCellValue(neutralCount + " (" + (totalWithSentiment > 0 ? String.format("%.1f", (neutralCount * 100.0 / totalWithSentiment)) : "0.0") + "%)");

        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Negative:");
        row.createCell(1).setCellValue(negativeCount + " (" + (totalWithSentiment > 0 ? String.format("%.1f", (negativeCount * 100.0 / totalWithSentiment)) : "0.0") + "%)");

        rowNum++;

        // PHẦN 4: PHÂN BỐ LOẠI THIỆT HẠI
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("DAMAGE CATEGORY DISTRIBUTION");
        cell.setCellStyle(boldStyle);

        Map<DamageCategory, Long> damageCounts = new HashMap<>();

        // Đếm từ Posts
        for (Post post : posts) {
            String cats = post.getDamageCategories();
            if (cats != null && !cats.isEmpty()) {
                for (String cat : cats.split(",")) {
                    try {
                        DamageCategory dc = DamageCategory.valueOf(cat.trim());
                        damageCounts.put(dc, damageCounts.getOrDefault(dc, 0L) + 1);
                    } catch (Exception ignored) {}
                }
            }
        }

        // Đếm từ Comments
        for (Comment comment : comments) {
            String cats = comment.getDamageCategories();
            if (cats != null && !cats.isEmpty()) {
                for (String cat : cats.split(",")) {
                    try {
                        DamageCategory dc = DamageCategory.valueOf(cat.trim());
                        damageCounts.put(dc, damageCounts.getOrDefault(dc, 0L) + 1);
                    } catch (Exception ignored) {}
                }
            }
        }

        for (DamageCategory category : DamageCategory.values()) {
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(category.getDisplayName() + ":");
            row.createCell(1).setCellValue(damageCounts.getOrDefault(category, 0L));
        }

        rowNum++;

        // Ngày xuất báo cáo
        row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("Export Date:");
        row.createCell(1).setCellValue(formatDateTime(LocalDateTime.now()));

        autoSizeColumns(sheet, 2);
    }

    private void createPostsSheet(Workbook workbook, List<Post> posts) {
        Sheet sheet = workbook.createSheet("Posts");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle contentStyle = createContentStyle(workbook);

        // Dòng Tiêu đề (Header)
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < POSTS_COLUMNS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(POSTS_COLUMNS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Đổ dữ liệu Bài viết
        int rowNum = 1;
        for (Post post : posts) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;

            row.createCell(colNum++).setCellValue(post.getId() != null ? String.valueOf(post.getId()) : "");
            row.createCell(colNum++).setCellValue(post.getPlatformId() != null ? post.getPlatformId() : "");
            row.createCell(colNum++).setCellValue(post.getPlatform() != null ? post.getPlatform() : "");

            Cell contentCell = row.createCell(colNum++);
            contentCell.setCellValue(post.getContent() != null ? post.getContent() : "");
            contentCell.setCellStyle(contentStyle);

            row.createCell(colNum++).setCellValue(post.getAuthor() != null ? post.getAuthor() : "");
            row.createCell(colNum++).setCellValue(formatDateTime(post.getPublishedAt()));
            row.createCell(colNum++).setCellValue(post.getUrl() != null ? post.getUrl() : "");
            row.createCell(colNum++).setCellValue(post.getSentiment() != null ? post.getSentiment().name() : "");

            Cell damageCell = row.createCell(colNum++);
            damageCell.setCellValue(formatDamageCategories(post.getDamageCategories()));
            damageCell.setCellStyle(contentStyle);

            Cell preprocessedCell = row.createCell(colNum++);
            preprocessedCell.setCellValue(post.getPreprocessedContent() != null ? post.getPreprocessedContent() : "");
            preprocessedCell.setCellStyle(contentStyle);

            row.createCell(colNum++).setCellValue(formatDateTime(post.getCollectedAt()));
        }

        freezeHeaderRow(sheet);
        autoSizeColumns(sheet, POSTS_COLUMNS.length);
    }

    private void createCommentsSheet(Workbook workbook, List<Comment> comments) {
        Sheet sheet = workbook.createSheet("Comments");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle contentStyle = createContentStyle(workbook);

        // Dòng Tiêu đề (Header)
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < COMMENTS_COLUMNS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(COMMENTS_COLUMNS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Đổ dữ liệu Bình luận
        int rowNum = 1;
        for (Comment comment : comments) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;

            row.createCell(colNum++).setCellValue(comment.getId() != null ? String.valueOf(comment.getId()) : "");
            row.createCell(colNum++).setCellValue(comment.getPostId() != null ? String.valueOf(comment.getPostId()) : "");
            row.createCell(colNum++).setCellValue(comment.getPlatformId() != null ? comment.getPlatformId() : "");
            row.createCell(colNum++).setCellValue(comment.getPlatform() != null ? comment.getPlatform() : "");

            Cell contentCell = row.createCell(colNum++);
            contentCell.setCellValue(comment.getContent() != null ? comment.getContent() : "");
            contentCell.setCellStyle(contentStyle);

            row.createCell(colNum++).setCellValue(comment.getAuthor() != null ? comment.getAuthor() : "");
            row.createCell(colNum++).setCellValue(formatDateTime(comment.getPublishedAt()));
            row.createCell(colNum++).setCellValue(comment.getSentiment() != null ? comment.getSentiment().name() : "");

            Cell damageCell = row.createCell(colNum++);
            damageCell.setCellValue(formatDamageCategories(comment.getDamageCategories()));
            damageCell.setCellStyle(contentStyle);

            Cell preprocessedCell = row.createCell(colNum++);
            preprocessedCell.setCellValue(comment.getPreprocessedContent() != null ? comment.getPreprocessedContent() : "");
            preprocessedCell.setCellStyle(contentStyle);

            row.createCell(colNum++).setCellValue(formatDateTime(comment.getCollectedAt()));
        }

        freezeHeaderRow(sheet);
        autoSizeColumns(sheet, COMMENTS_COLUMNS.length);
    }
}