package com.disaster.analysis.infrastructure.analysis;

import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.contract.analysis.DamageClassifier;

import java.util.*;

/**
 * Công cụ phân loại từ khóa (Keyword-based) để xác định hạng mục thiệt hại của thảm họa.
 */
public class KeywordDamageClassifier implements DamageClassifier {

    private boolean initialized = false;
    private static final Map<DamageCategory, List<String>> CATEGORY_KEYWORDS = new HashMap<>();

    static {
        CATEGORY_KEYWORDS.put(DamageCategory.PEOPLE_AFFECTED, Arrays.asList(
                "death", "deaths", "died", "killed", "casualty", "casualties", "fatality",
                "chết", "tử vong", "thiệt mạng", "người chết", "thi thể",
                "bị thương", "thương vong", "thương tích", "người bị thương",
                "mất tích", "mất liên lạc", "thất lạc",
                "sơ tán", "di dời", "di tản", "người sơ tán",
                "nạn nhân", "người dân", "sống sót", "cứu hộ", "giải cứu", "cứu nạn"
        ));

        CATEGORY_KEYWORDS.put(DamageCategory.ECONOMIC_DISRUPTION, Arrays.asList(
                "business", "businesses", "closed", "shutdown", "bankrupt", "economic",
                "kinh tế", "tài chính", "thiệt hại kinh tế", "doanh nghiệp", "công ty", "cửa hàng",
                "đóng cửa", "phá sản", "ngừng hoạt động", "thất nghiệp", "mất việc", "sa thải",
                "nông nghiệp", "mùa màng", "vụ mùa", "thu hoạch", "cây trồng",
                "chăn nuôi", "gia súc", "nuôi trồng thủy sản", "nghề cá",
                "sản xuất", "nhà máy", "xí nghiệp", "thương mại", "buôn bán"
        ));

        CATEGORY_KEYWORDS.put(DamageCategory.BUILDING_DAMAGE, Arrays.asList(
                "building", "buildings", "house", "houses", "home", "collapsed", "destroyed",
                "nhà", "nhà cửa", "căn nhà", "ngôi nhà", "tòa nhà",
                "sập", "đổ sập", "sụp đổ", "đổ nát", "hư hại", "hư hỏng", "tàn phá",
                "nứt", "vết nứt", "rạn nứt", "sụt lún", "mái nhà", "tường", "móng",
                "chung cư", "trường học", "bệnh viện", "phòng khám", "chùa", "nhà thờ",
                "ngập", "ngập lụt", "chìm trong nước"
        ));

        CATEGORY_KEYWORDS.put(DamageCategory.PERSONAL_PROPERTY_LOSS, Arrays.asList(
                "belongings", "possessions", "property", "furniture", "car", "vehicle",
                "tài sản", "của cải", "đồ đạc", "vật dụng", "đồ dùng", "nội thất", "thiết bị",
                "xe", "xe máy", "xe hơi", "ô tô", "phương tiện", "quần áo",
                "mất hết", "mất sạch", "trôi hết", "cuốn trôi",
                "giấy tờ", "tiền bạc", "tiền tiết kiệm", "thực phẩm", "lương thực"
        ));

        CATEGORY_KEYWORDS.put(DamageCategory.INFRASTRUCTURE_DAMAGE, Arrays.asList(
                "road", "roads", "highway", "bridge", "power", "electricity", "water supply",
                "đường", "đường sá", "đường bộ", "quốc lộ", "cầu", "cống", "hầm",
                "điện", "điện lực", "lưới điện", "đường dây điện", "mất điện", "cúp điện",
                "nước", "cấp nước", "hệ thống nước", "đường ống",
                "thông tin", "liên lạc", "mạng", "internet", "đường sắt", "nhà ga",
                "sân bay", "bến cảng", "đập", "đê", "đê điều", "kè", "cống rãnh"
        ));

        CATEGORY_KEYWORDS.put(DamageCategory.OTHER, Arrays.asList(
                "disaster", "catastrophe", "emergency", "storm", "flood",
                "thiên tai", "thảm họa", "bão", "lũ", "lụt", "sạt lở", "động đất", "sóng thần",
                "thiệt hại", "tàn phá", "ảnh hưởng", "cứu trợ", "hỗ trợ", "giúp đỡ",
                "khắc phục", "phục hồi", "tái thiết", "xây dựng lại"
        ));
    }

    @Override
    public void initialize() {
        if (!initialized) {
            initialized = true;
        }
    }

    @Override
    public Set<DamageCategory> classifyDamage(String text) {
        if (text == null || text.trim().isEmpty()) return Collections.emptySet();

        String normalizedText = text.toLowerCase();
        Map<DamageCategory, Integer> categoryScores = new HashMap<>();

        for (Map.Entry<DamageCategory, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                String lowerKeyword = keyword.toLowerCase();
                int index = 0;
                while ((index = normalizedText.indexOf(lowerKeyword, index)) != -1) {
                    score++;
                    index += lowerKeyword.length();
                }
            }
            categoryScores.put(entry.getKey(), score);
        }

        Set<DamageCategory> matchedCategories = new HashSet<>();
        for (Map.Entry<DamageCategory, Integer> entry : categoryScores.entrySet()) {
            if (entry.getValue() > 0 && entry.getKey() != DamageCategory.OTHER) {
                matchedCategories.add(entry.getKey());
            }
        }

        if (matchedCategories.isEmpty() && categoryScores.getOrDefault(DamageCategory.OTHER, 0) > 0) {
            matchedCategories.add(DamageCategory.OTHER);
        }

        return matchedCategories;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }
}