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

        summary.append("<header>=== BÁO CÁO TỔNG HỢP TÌNH HÌNH (CHẾ ĐỘ MÔ PHỎNG) ===\n\n");

        summary.append("## 1. CÁC CHỦ ĐỀ CHÍNH ĐƯỢC QUAN TÂM\n");
        summary.append("Dự án \"yagi\" đã tiến hành quét dữ liệu mạng xã hội liên quan đến sự kiện/thảm họa \"yagi\" trong khoảng thời gian từ 00:00 ngày 02/06/2026 đến 00:00 ngày 06/06/2026.\n");
        summary.append("Tổng cộng 46 bài viết và 1559 bình luận đã được thu thập và phân tích, cho thấy bức tranh đa chiều về tác động của thảm họa và phản ứng của cộng đồng trong giai đoạn khắc phục hậu quả ban đầu.\n\n");
        summary.append("</header>");

        summary.append("<sentiment>");
        summary.append("## 2. PHÂN TÍCH CẢM XÚC CỘNG ĐỒNG\n");
        summary.append("Biểu đồ cảm xúc cộng đồng cho thấy tâm lý phức tạp nhưng có xu hướng trung lập và tích cực chiếm ưu thế, với 41.0% trung lập (658 lượt), 33.5% tích cực (538 lượt) và 25.5% tiêu cực (409 lượt).\n\n");

        summary.append("Chỉ số trung lập cao phản ánh sự lan truyền mạnh mẽ của các thông tin cập nhật tình hình bão, lũ lụt và sạt lở, cũng như các thông báo chính thức về hoạt động cứu trợ và quyên góp. Điều này cho thấy cộng đồng đang rất quan tâm đến việc nắm bắt thông tin chính xác và hướng dẫn hành động trong bối cảnh thảm họa.\n\n");

        summary.append("Cảm xúc tích cực chủ yếu đến từ các tin tức về công tác cứu hộ, sự hỗ trợ từ cộng đồng và tinh thần vượt khó. Nhiều bài viết ghi nhận việc cứu hộ thành công các hộ gia đình bị mắc kẹt, các hoạt động cứu trợ diễn ra nhanh chóng cùng sự tri ân đối với lực lượng cứu hộ, quân đội và tình nguyện viên.\n\n");

        summary.append("Trong khi đó, cảm xúc tiêu cực xuất phát từ những thông tin về mức độ thiệt hại nặng nề, sự mất mát và tình trạng khó khăn của người dân tại các khu vực bị ảnh hưởng nghiêm trọng. Các nội dung về sạt lở đất, nhà cửa bị cuốn trôi, mất điện, mất sóng và thiếu lương thực đã gây ra tâm lý lo lắng và thương cảm sâu sắc.\n\n");

        summary.append("Để ổn định tâm lý người dân, cần tiếp tục công bố thông tin cập nhật chính xác về tình hình khắc phục hậu quả, đồng thời lan tỏa nhiều hơn các câu chuyện tích cực về tinh thần đoàn kết và tái thiết sau thiên tai.\n\n");
        summary.append("</sentiment>");

        summary.append("<damage>");
        summary.append("## 3. PHÂN LOẠI THIỆT HẠI NỔI BẬT\n");
        summary.append("Các hạng mục thiệt hại được nhắc tới nhiều nhất là Infrastructure Damage (774 lượt) và Building Damage (513 lượt), cho thấy cơ sở hạ tầng và nhà cửa là hai lĩnh vực chịu ảnh hưởng nặng nề nhất. Tiếp theo là People Affected (405 lượt), Economic Disruption (224 lượt) và Personal Property Loss (190 lượt).\n\n");

        summary.append("Thiệt hại cơ sở hạ tầng bao gồm các trường hợp sạt lở quốc lộ, sập cầu, công trình hư hỏng, đứt cáp quang và mất điện diện rộng, khiến nhiều khu vực bị cô lập.\n\n");

        summary.append("Thiệt hại nhà cửa được phản ánh qua các trường hợp nhà bị lũ cuốn trôi, nhà cửa đổ sập, trường học tốc mái và nhiều công trình dân sinh bị hư hỏng nghiêm trọng.\n\n");

        summary.append("Số lượng lớn người dân bị ảnh hưởng thể hiện qua các báo cáo về người dân mắc kẹt, các hộ gia đình bị cô lập và những khu vực không thể tiếp cận do giao thông bị chia cắt.\n\n");

        summary.append("Thiệt hại kinh tế được thể hiện qua tình trạng hàng quán đóng cửa, chuỗi cung ứng gián đoạn, doanh nghiệp gặp khó khăn và thiệt hại trong sản xuất nông nghiệp, chăn nuôi.\n\n");

        summary.append("Từ kết quả phân tích, ưu tiên hàng đầu là khôi phục cơ sở hạ tầng thiết yếu, hỗ trợ khẩn cấp cho người dân bị ảnh hưởng và triển khai các kế hoạch tái thiết nhà cửa, công trình công cộng.\n\n");
        summary.append("</damage>");

        summary.append("<recommendation>");
        summary.append("## 4. ĐỀ XUẤT HÀNH ĐỘNG\n");
        summary.append("1. Cực kỳ khẩn cấp: Tổ chức các đội cứu hộ chuyên nghiệp để giải cứu những người còn mắc kẹt tại các vùng bị cô lập và cung cấp ngay các nhu yếu phẩm thiết yếu.\n\n");

        summary.append("2. Khẩn cấp: Khôi phục nhanh các tuyến giao thông, hệ thống điện và thông tin liên lạc nhằm kết nối lại các khu vực bị chia cắt và hỗ trợ vận chuyển hàng cứu trợ.\n\n");

        summary.append("3. Quan trọng: Đánh giá toàn diện thiệt hại nhà cửa, trường học và công trình công cộng để xây dựng kế hoạch tái thiết phù hợp.\n\n");

        summary.append("4. Trung bình: Hỗ trợ người dân, hộ kinh doanh và doanh nghiệp khôi phục hoạt động sản xuất, kinh doanh nhằm ổn định sinh kế và kinh tế địa phương.\n\n");

        summary.append("5. Dài hạn: Xây dựng các chương trình phòng chống thiên tai bền vững, nâng cao nhận thức cộng đồng và đầu tư vào cơ sở hạ tầng có khả năng chống chịu tốt hơn trước các hiện tượng thời tiết cực đoan.\n\n");

        summary.append("---\n");
        summary.append("*LƯU Ý: Đây chỉ là bản báo cáo giả lập (Mock) phục vụ mục đích kiểm thử phần mềm. ");
        summary.append("Vui lòng cấu hình GEMINI_API_KEY hoặc OPENAI_API_KEY trong file .env để AI thật xử lý dữ liệu thực tế.*\n");

        summary.append("---\n");
        summary.append("*LƯU Ý: Đây chỉ là bản báo cáo giả lập (Mock) phục vụ mục đích kiểm thử phần mềm. ");
        summary.append("Vui lòng cấu hình GEMINI_API_KEY hoặc OPENAI_API_KEY trong file .env để AI thật xử lý dữ liệu thực tế.*\n");
        summary.append("</recommendation>");
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