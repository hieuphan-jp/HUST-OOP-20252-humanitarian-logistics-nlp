package com.disaster.analysis.domain.repository;

import com.disaster.analysis.domain.model.AISummary;
import java.util.Optional;

/**
 * Cổng giao tiếp để lưu trữ và truy xuất các Báo cáo tổng hợp do AI sinh ra.
 */
public interface SummaryRepository {

    /** * Lưu mới hoặc cập nhật một báo cáo AI vào cơ sở dữ liệu.
     * @param summary Đối tượng báo cáo.
     * @return Báo cáo đã lưu.
     */
    AISummary save(AISummary summary);

    /** * Lấy báo cáo AI của một dự án.
     * @param projectId ID của dự án.
     * @return Báo cáo AI (nếu có).
     */
    Optional<AISummary> findByProjectId(Long projectId);

    /** * Xóa báo cáo AI của một dự án (Dùng khi muốn yêu cầu AI viết lại một báo cáo mới).
     */
    void deleteByProjectId(Long projectId);
}