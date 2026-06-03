package com.disaster.analysis.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Công cụ bóc tách và tự động lắp ráp báo cáo AI.
 * Giúp trích xuất các phần phân tích riêng biệt và đóng gói thành một văn bản
 * báo cáo hoàn chỉnh (Đầy đủ Header - Body - Recommendation) cho từng trang biểu đồ.
 */
public class AISummaryParser {

    /**
     * Hàm lõi dùng để trích xuất nội dung nằm giữa một cặp thẻ XML giả.
     */
    public static String extractSection(String fullSummaryText, String tagName) {
        if (fullSummaryText == null || fullSummaryText.isEmpty()) {
            return "";
        }

        // Tạo Regex tìm kiếm nội dung nằm giữa <tagName> và </tagName>
        // Cờ (?s) (Pattern.DOTALL) giúp quét xuyên suốt qua các ký tự xuống dòng (\n)
        String regex = "<" + tagName + ">(.*?)</" + tagName + ">";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(fullSummaryText);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * LẮP RÁP BÁO CÁO CẢM XÚC HOÀN CHỈNH
     * Cấu trúc: [Header Dự Án] -> [Phân tích Cảm xúc chuyên sâu] -> [5 Hành động khẩn cấp]
     *
     * @param fullSummaryText Toàn bộ nội dung thô lấy từ Database.
     * @return Văn bản báo cáo cảm xúc đồng bộ hoàn chỉnh để in lên giao diện.
     */
    public static String getCompleteSentimentReport(String fullSummaryText) {
        String header = extractSection(fullSummaryText, "header");
        String sentiment = extractSection(fullSummaryText, "sentiment");
        String recommendation = extractSection(fullSummaryText, "recommendation");

        StringBuilder report = new StringBuilder();

        // 1. Gán phần mở bài
        if (!header.isEmpty()) {
            report.append("╔════════════════════════════════════════════════════╗\n");
            report.append("║                  THÔNG TIN TỔNG QUAN               ║");
            report.append("╚════════════════════════════════════════════════════╝\n\n");
            report.append(header).append("\n\n");
        }

        // 2. Gán phần thân bài chuyên sâu về cảm xúc
        report.append("╔════════════════════════════════════════════════════╗\n");
        report.append("║       PHÂN TÍCH TÂM LÝ & CẢM XÚC CỘNG ĐỒNG         ║");
        report.append("╚════════════════════════════════════════════════════╝\n\n");
        if (!sentiment.isEmpty()) {
            report.append(sentiment).append("\n\n");
        } else {
            report.append("⚠ Chưa có dữ liệu phân tích cảm xúc cho mục này.\n\n");
        }

        // 3. Gán phần kết luận cứu hộ khẩn cấp
        if (!recommendation.isEmpty()) {
            report.append("┌────────────────────────────────────────────────────┐\n");
            report.append("│        ĐỀ XUẤT CÁC HÀNH ĐỘNG ỨNG PHÓ KHẨN CẤP      │\n");
            report.append("└────────────────────────────────────────────────────┘\n");
            report.append(recommendation);
        }

        return report.toString().trim();
    }

    /**
     * LẮP RÁP BÁO CÁO THIỆT HẠI HOÀN CHỈNH
     * Cấu trúc: [Header Dự Án] -> [Phân tích Thiệt hại chuyên sâu] -> [5 Hành động khẩn cấp]
     *
     * @param fullSummaryText Toàn bộ nội dung thô lấy từ Database.
     * @return Văn bản báo cáo thiệt hại đồng bộ hoàn chỉnh để in lên giao diện.
     */
    public static String getCompleteDamageReport(String fullSummaryText) {
        String header = extractSection(fullSummaryText, "header");
        String damage = extractSection(fullSummaryText, "damage");
        String recommendation = extractSection(fullSummaryText, "recommendation");

        StringBuilder report = new StringBuilder();

        // 1. Gán phần mở bài
        if (!header.isEmpty()) {
            report.append("╔════════════════════════════════════════════════════╗\n");
            report.append("║                  THÔNG TIN TỔNG QUAN               ║");
            report.append("╚════════════════════════════════════════════════════╝\n\n");
            report.append(header).append("\n\n");
        }

        // 2. Gán phần thân bài chuyên sâu về hạng mục thiệt hại
        report.append("╔════════════════════════════════════════════════════╗\n");
        report.append("║       PHÂN TÍCH CÁC HẠNG MỤC THIỆT HẠI CHÍNH       ║\n");
        report.append("╚════════════════════════════════════════════════════╝\n\n");
        if (!damage.isEmpty()) {
            report.append(damage).append("\n\n");
        } else {
            report.append("⚠ Chưa có dữ liệu phân tích thiệt hại hạ tầng cho mục này.\n\n");
        }

        // 3. Gán phần kết luận cứu hộ khẩn cấp
        if (!recommendation.isEmpty()) {
            report.append("┌────────────────────────────────────────────────────┐\n");
            report.append("│        ĐỀ XUẤT CÁC HÀNH ĐỘNG ỨNG PHÓ KHẨN CẤP      │\n");
            report.append("└────────────────────────────────────────────────────┘\n");
            report.append(recommendation);
        }

        return report.toString().trim();
    }
}