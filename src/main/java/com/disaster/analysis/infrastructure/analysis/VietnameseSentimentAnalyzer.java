package com.disaster.analysis.infrastructure.analysis;

import com.disaster.analysis.domain.model.enums.Sentiment;
import com.disaster.analysis.domain.contract.analysis.SentimentAnalyzer;

import java.util.*;

/**
 * Công cụ phân tích cảm xúc dành riêng cho Tiếng Việt.
 * Sử dụng phương pháp đối chiếu từ điển (Dictionary-based) kết hợp trọng số.
 */
public class VietnameseSentimentAnalyzer implements SentimentAnalyzer {

    private boolean initialized = false;

    private static final Set<String> POSITIVE_WORDS = new HashSet<>();
    private static final Set<String> NEGATIVE_WORDS = new HashSet<>();
    private static final Set<String> NEGATION_WORDS = new HashSet<>();
    private static final Set<String> INTENSIFIERS = new HashSet<>();

    static {
        // Từ khóa tích cực (Hy vọng, cứu trợ, an toàn)
        POSITIVE_WORDS.addAll(Arrays.asList(
                "hy vọng", "lạc quan", "tin tưởng", "tích cực", "vui", "vui mừng", "hạnh phúc",
                "may mắn", "tốt", "tốt đẹp", "tuyệt vời", "xuất sắc", "hoàn hảo",
                "phục hồi", "cải thiện", "tiến bộ", "khắc phục", "ổn định", "an toàn",
                "bình yên", "yên tâm", "thanh bình", "hòa bình",
                "giúp đỡ", "hỗ trợ", "cứu trợ", "cứu hộ", "cứu nạn", "chia sẻ",
                "đoàn kết", "đồng lòng", "chung tay", "góp sức", "đóng góp",
                "tình nguyện", "thiện nguyện", "nhân đạo", "từ thiện",
                "thành công", "đạt được", "hoàn thành", "giải quyết", "vượt qua",
                "chiến thắng", "sống sót", "thoát nạn", "cảm ơn", "biết ơn", "tri ân",
                "cảm kích", "cảm động", "tốt bụng", "tử tế", "tận tâm", "nhiệt tình", "chu đáo",
                "hiệu quả", "kịp thời", "nhanh chóng", "đầy đủ", "chuyên nghiệp", "tận tình", "tận lực"
        ));

        // Từ khóa tiêu cực (Thương vong, thiệt hại, sợ hãi)
        NEGATIVE_WORDS.addAll(Arrays.asList(
                "chết", "tử vong", "thiệt mạng", "thi thể", "nạn nhân", "thương vong",
                "hy sinh", "mất mạng", "qua đời", "chết người",
                "bị thương", "thương tích", "đau đớn", "đau khổ", "khổ sở", "chịu đựng",
                "tổn thương", "bị nạn", "gặp nạn", "tai nạn",
                "phá hủy", "tàn phá", "hủy hoại", "đổ nát", "sụp đổ", "sập", "đổ sập",
                "hư hại", "hư hỏng", "thiệt hại", "mất mát", "tổn thất",
                "tan hoang", "hoang tàn", "điêu tàn", "tiêu điều",
                "thảm họa", "thảm khốc", "thảm kịch", "tai họa", "thiên tai",
                "bão", "lũ", "lụt", "lũ lụt", "ngập lụt", "sạt lở", "động đất",
                "hỏa hoạn", "cháy", "cháy nổ",
                "sợ hãi", "hoảng loạn", "hoảng sợ", "kinh hoàng", "khủng khiếp",
                "đáng sợ", "ghê gớm", "dữ dội", "hung dữ", "tàn khốc",
                "mất tích", "thất lạc", "mất liên lạc", "biệt tích", "mất",
                "mất hết", "mất sạch", "trôi hết", "cuốn trôi", "cuốn phăng",
                "buồn", "đau buồn", "thương tâm", "thương tiếc", "tiếc thương",
                "đau lòng", "xót xa", "thương xót", "thương cảm",
                "tuyệt vọng", "vô vọng", "chán nản", "nản lòng",
                "nguy hiểm", "nguy cấp", "nguy kịch", "hiểm nghèo", "rủi ro",
                "đe dọa", "đáng lo", "lo lắng", "lo ngại",
                "nghèo", "nghèo khó", "khó khăn", "cơ cực", "túng thiếu",
                "thiếu thốn", "đói", "đói khát", "khát", "rét",
                "bỏ rơi", "bị bỏ rơi", "cô đơn", "cô lập", "bị cô lập",
                "không có", "thiếu", "không đủ", "chậm trễ", "trì hoãn",
                "tồi tệ", "tệ hại", "xấu", "kém", "yếu", "không tốt",
                "vấn đề", "sự cố", "trục trặc", "hỏng", "lỗi",
                "chậm", "không kịp", "muộn", "trễ"
        ));

        // Từ phủ định (Đảo ngược cảm xúc)
        NEGATION_WORDS.addAll(Arrays.asList(
                "không", "chẳng", "chưa", "chả", "không có",
                "không phải", "chẳng phải", "chưa từng", "chưa bao giờ",
                "không bao giờ", "không còn", "không thể", "không được"
        ));

        // Từ nhấn mạnh (Tăng trọng số)
        INTENSIFIERS.addAll(Arrays.asList(
                "rất", "vô cùng", "cực kỳ", "hết sức", "quá", "quá đỗi",
                "thật", "thật sự", "thực sự", "hoàn toàn", "tuyệt đối",
                "cực", "siêu", "hơn", "nhiều", "lắm", "mạnh", "nặng", "nghiêm trọng"
        ));
    }

    @Override
    public void initialize() {
        if (!initialized) {
            if (POSITIVE_WORDS.isEmpty() || NEGATIVE_WORDS.isEmpty()) {
                throw new IllegalStateException("Từ điển phân tích cảm xúc chưa được nạp.");
            }
            initialized = true;
        }
    }

    @Override
    public Sentiment analyzeSentiment(String text) {
        if (!initialized) {
            throw new IllegalStateException("Cần gọi initialize() trước khi phân tích.");
        }

        if (text == null || text.trim().isEmpty()) {
            return Sentiment.NEUTRAL;
        }

        String normalizedText = text.toLowerCase();
        String[] words = normalizedText.split("\\s+");

        int positiveScore = 0;
        int negativeScore = 0;
        boolean negated = false;
        double intensifierMultiplier = 1.0;

        for (int i = 0; i < words.length; i++) {
            String word = words[i];

            if (NEGATION_WORDS.contains(word)) {
                negated = true;
                continue;
            }

            if (INTENSIFIERS.contains(word)) {
                intensifierMultiplier = 1.5;
                continue;
            }

            String twoWordPhrase = (i < words.length - 1) ? word + " " + words[i + 1] : null;
            String threeWordPhrase = (i < words.length - 2) ? word + " " + words[i + 1] + " " + words[i + 2] : null;

            boolean matched = false;

            if (threeWordPhrase != null) {
                if (POSITIVE_WORDS.contains(threeWordPhrase)) {
                    positiveScore += negated ? -(int)(2 * intensifierMultiplier) : (int)(2 * intensifierMultiplier);
                    matched = true;
                    i += 2;
                } else if (NEGATIVE_WORDS.contains(threeWordPhrase)) {
                    negativeScore += negated ? -(int)(2 * intensifierMultiplier) : (int)(2 * intensifierMultiplier);
                    matched = true;
                    i += 2;
                }
            }

            if (!matched && twoWordPhrase != null) {
                if (POSITIVE_WORDS.contains(twoWordPhrase)) {
                    positiveScore += negated ? -(int)(2 * intensifierMultiplier) : (int)(2 * intensifierMultiplier);
                    matched = true;
                    i++;
                } else if (NEGATIVE_WORDS.contains(twoWordPhrase)) {
                    negativeScore += negated ? -(int)(2 * intensifierMultiplier) : (int)(2 * intensifierMultiplier);
                    matched = true;
                    i++;
                }
            }

            if (!matched) {
                if (POSITIVE_WORDS.contains(word)) {
                    positiveScore += negated ? -(int)intensifierMultiplier : (int)intensifierMultiplier;
                } else if (NEGATIVE_WORDS.contains(word)) {
                    negativeScore += negated ? -(int)intensifierMultiplier : (int)intensifierMultiplier;
                }
            }

            if (matched || POSITIVE_WORDS.contains(word) || NEGATIVE_WORDS.contains(word)) {
                negated = false;
                intensifierMultiplier = 1.0;
            }
        }

        int totalScore = positiveScore - negativeScore;

        // Vì ngữ cảnh thảm họa thường tiêu cực, ta hạ ngưỡng Negative xuống một chút
        if (totalScore > 1) {
            return Sentiment.POSITIVE;
        } else if (totalScore < -1) {
            return Sentiment.NEGATIVE;
        } else {
            return Sentiment.NEUTRAL;
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }
}