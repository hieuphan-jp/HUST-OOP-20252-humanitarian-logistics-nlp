package com.disaster.analysis.domain.contract.analysis;

import com.disaster.analysis.domain.model.enums.DamageCategory;
import java.util.Set;

/**
 * Bản hợp đồng định nghĩa bộ phân loại thiệt hại dựa trên nội dung văn bản.
 */
public interface DamageClassifier {

    /**
     * Nạp các từ khóa nhận diện thiệt hại (Ví dụ: "sập", "cuốn trôi") vào bộ nhớ.
     */
    void initialize();

    /**
     * Quét nội dung văn bản và dán nhãn các loại thiệt hại được nhắc đến.
     *
     * @param text Nội dung bài viết.
     * @return Một tập hợp (Set) chứa các loại thiệt hại. Có thể trả về Set rỗng nếu không có thiệt hại nào.
     */
    Set<DamageCategory> classifyDamage(String text);

    /**
     * Kiểm tra xem bộ phân loại đã sẵn sàng hoạt động chưa.
     */
    boolean isInitialized();
}