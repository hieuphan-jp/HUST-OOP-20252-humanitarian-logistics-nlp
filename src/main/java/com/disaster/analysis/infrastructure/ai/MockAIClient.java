package com.disaster.analysis.infrastructure.ai;

import com.disaster.analysis.domain.exception.AIClientException;
import com.disaster.analysis.domain.contract.ai.AIClient;

/**
 * Lớp AI giả lập (Mock).
 * Dùng để test UI và quy trình hệ thống khi không có mạng hoặc chưa điền API Key.
 */
public class MockAIClient implements AIClient {

    @Override
    public String generateSummary(String prompt) throws AIClientException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new AIClientException("Dữ liệu đầu vào không được để trống");
        }

        // Tạo một báo cáo giả định bằng Tiếng Việt rất chuẩn form
        StringBuilder summary = new StringBuilder();

        summary.append("=== BÁO CÁO TỔNG HỢP TÌNH HÌNH (CHẾ ĐỘ MÔ PHỎNG) ===\n\n");

        summary.append("## 1. CÁC CHỦ ĐỀ CHÍNH ĐƯỢC QUAN TÂM\n");
        summary.append("Dựa trên phân tích dữ liệu mạng xã hội, các chủ đề sau đang được thảo luận nhiều nhất:\n\n");
        summary.append("- **Cảnh báo an toàn & Sơ tán:** Cộng đồng đang tích cực chia sẻ thông tin về các khu vực an toàn, tuyến đường chưa bị ngập và số điện thoại cứu hộ.\n");
        summary.append("- **Cập nhật thiệt hại:** Xuất hiện nhiều bài đăng báo cáo tình trạng cây đổ, tốc mái và ngập úng diện rộng tại các vùng trũng.\n");
        summary.append("- **Tinh thần tương thân tương ái:** Rất nhiều bài viết kêu gọi quyên góp áo phao, xuồng máy và thực phẩm cứu trợ.\n\n");

        summary.append("## 2. PHÂN LOẠI THIỆT HẠI NỔI BẬT\n");
        summary.append("Dữ liệu chỉ ra các khu vực/hạng mục bị ảnh hưởng nghiêm trọng nhất:\n\n");
        summary.append("- **Hạ tầng lưới điện:** Mất điện diện rộng được nhắc đến trong 60% bình luận khu vực tâm bão.\n");
        summary.append("- **Giao thông:** Cây đổ và ngập lụt gây tắc nghẽn, cô lập một số tuyến đường huyết mạch.\n");
        summary.append("- **Nông nghiệp:** Thiệt hại hoa màu và ngập úng lồng bè nuôi trồng thủy sản.\n\n");

        summary.append("## 3. PHÂN TÍCH CẢM XÚC CỘNG ĐỒNG\n");
        summary.append("- **Tiêu cực (Lo âu, Sợ hãi):** Chiếm tỷ lệ cao ở giai đoạn đầu, thể hiện sự lo lắng về an toàn của người thân và tài sản.\n");
        summary.append("- **Tích cực (Lạc quan, Biết ơn):** Đang có xu hướng tăng nhờ các hoạt động cứu trợ kịp thời từ chính quyền và các đoàn thiện nguyện.\n\n");

        summary.append("## 4. ĐỀ XUẤT HÀNH ĐỘNG\n");
        summary.append("1. Khẩn trương khôi phục mạng lưới thông tin liên lạc và điện lưới tại các khu vực bị cô lập.\n");
        summary.append("2. Điều phối lực lượng chức năng dọn dẹp chướng ngại vật trên các tuyến đường chính.\n");
        summary.append("3. Thiết lập hệ thống cung cấp thông tin chính thống để bác bỏ các tin giả (fake news) gây hoang mang dư luận.\n\n");

        summary.append("---\n");
        summary.append("*LƯU Ý: Đây chỉ là bản báo cáo giả lập (Mock) phục vụ mục đích kiểm thử phần mềm. ");
        summary.append("Vui lòng cấu hình GEMINI_API_KEY hoặc OPENAI_API_KEY trong file .env để AI thật xử lý dữ liệu thực tế.*\n");

        return summary.toString();
    }

    @Override
    public String getModelName() {
        return "mock-ai-offline";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}