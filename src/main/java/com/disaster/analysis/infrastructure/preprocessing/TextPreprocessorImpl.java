package com.disaster.analysis.infrastructure.preprocessing;

import com.disaster.analysis.domain.contract.preprocessing.TextPreprocessor;

import java.util.regex.Pattern;

/**
 * Lớp thực thi (Implementation) cho hợp đồng TextPreprocessor.
 * Chịu trách nhiệm "tẩy rửa" và chuẩn hóa dữ liệu thô cào từ mạng xã hội
 * trước khi đưa vào các mô hình Trí tuệ nhân tạo (NLP) để phân tích cảm xúc.
 */
public class TextPreprocessorImpl implements TextPreprocessor {

    // Regex tìm các đường link URL (http, https)
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+\\s?");

    // Regex tìm các thẻ HTML lộn xộn (VD: <br>, <b>, </a>)
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    // Regex giữ lại chữ cái, số, khoảng trắng và các dấu câu cơ bản. Xóa bỏ Emoji, ký tự lạ.
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^\\p{L}\\p{N}\\s.,!?;:'\"-]");

    // Regex tìm các khoảng trắng, dấu tab, dấu enter thừa thãi liên tiếp nhau
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * Hàm chính thực thi toàn bộ luồng (pipeline) làm sạch văn bản.
     * * @param rawContent Dữ liệu thô cào từ mạng xã hội (Post, Comment).
     * @return Chuỗi văn bản đã được chuẩn hóa (chữ thường, sạch sẽ, gọn gàng).
     */
    @Override
    public String preprocess(String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            return "";
        }

        String text = rawContent;

        // Bước 1: Xóa bỏ các thẻ HTML (thay bằng khoảng trắng để các từ không bị dính vào nhau)
        text = HTML_TAG_PATTERN.matcher(text).replaceAll(" ");

        // Bước 2: Xóa bỏ các đường link URL làm nhiễu ngữ nghĩa
        text = URL_PATTERN.matcher(text).replaceAll("");

        // Bước 3: Lọc bỏ emoji và các ký tự đặc biệt, chỉ giữ lại chữ, số và dấu câu
        text = SPECIAL_CHARS_PATTERN.matcher(text).replaceAll("");

        // Bước 4: Chuyển toàn bộ về chữ thường để đồng bộ hóa từ vựng (VD: "Bão" = "bão")
        text = text.toLowerCase();

        // Bước 5: Cắt tỉa các khoảng trắng thừa ở giữa và hai đầu
        return WHITESPACE_PATTERN.matcher(text.trim()).replaceAll(" ");
    }

    /**
     * Hàm tiện ích: Chỉ xóa ký tự đặc biệt (Dùng khi cần giữ nguyên cấu trúc câu chữ gốc).
     * * @param text Văn bản cần xử lý.
     * @return Văn bản không còn ký tự đặc biệt.
     */
    public static String removeSpecialCharacters(String text) {
        if (text == null) {
            return "";
        }
        return SPECIAL_CHARS_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Hàm tiện ích: Chỉ nén khoảng trắng (Dùng khi cần làm gọn giao diện hiển thị).
     * * @param text Văn bản cần xử lý.
     * @return Văn bản với các từ chỉ cách nhau đúng 1 dấu cách.
     */
    public static String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return WHITESPACE_PATTERN.matcher(text.trim()).replaceAll(" ");
    }
}