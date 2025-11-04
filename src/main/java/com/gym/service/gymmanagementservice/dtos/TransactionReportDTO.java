package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.PaymentMethod;
import com.gym.service.gymmanagementservice.models.Transaction;
import com.gym.service.gymmanagementservice.models.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class TransactionReportDTO {
    private Long id;
    private OffsetDateTime transactionDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private TransactionStatus status;
    private String createdByStaffName; // Tên nhân viên
    private String transactionType; // "Gói tập" hay "Sản phẩm"
    private String description; // Tên gói tập / ID hóa đơn

    /**
     * Hàm chuyển đổi từ Entity Transaction sang DTO
     */
    public static TransactionReportDTO fromTransaction(Transaction tx) {
        String type = "Không xác định";
        String desc = "N/A";

        if (tx.getMemberPackage() != null) {
            type = "Gói tập";
            // Tải tên gói (Lưu ý: có thể gây N+1 query nếu không Eager)
            desc = tx.getMemberPackage().getGymPackage().getName();
        } else if (tx.getSale() != null) {
            type = "Bán lẻ (POS)";
            desc = "Hóa đơn #" + tx.getSale().getId();
        }

        return TransactionReportDTO.builder()
                .id(tx.getId())
                .transactionDate(tx.getTransactionDate())
                .amount(tx.getAmount())
                .paymentMethod(tx.getPaymentMethod())
                .status(tx.getStatus())
                // Tải tên nhân viên (Lưu ý: có thể gây N+1 query nếu không Eager)
                .createdByStaffName(tx.getCreatedBy().getFullName())
                .transactionType(type)
                .description(desc)
                .build();
    }
}