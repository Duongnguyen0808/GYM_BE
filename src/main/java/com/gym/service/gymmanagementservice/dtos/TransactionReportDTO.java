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
    private String buyerName;

    /**
     * Hàm chuyển đổi từ Entity Transaction sang DTO
     */
    public static TransactionReportDTO fromTransaction(Transaction tx) {
        String type = "Không xác định";
        String desc = "N/A";
        String buyer = "-";

        if (tx.getMemberPackage() != null) {
            // Phân loại theo TransactionKind
            if (tx.getKind() != null) {
                switch (tx.getKind()) {
                    case SUBSCRIPTION_NEW:
                        type = "Gói tập";
                        break;
                    case SUBSCRIPTION_RENEW:
                        type = "Gia hạn";
                        break;
                    case SUBSCRIPTION_UPGRADE:
                        type = "Nâng cấp";
                        break;
                    case REFUND:
                        type = "Hoàn tiền";
                        break;
                    default:
                        type = "Gói tập";
                }
            } else {
                type = "Gói tập";
            }
            
            // Tải tên gói (Lưu ý: có thể gây N+1 query nếu không Eager)
            desc = tx.getMemberPackage().getGymPackage().getName();
            buyer = tx.getMemberPackage().getMember() != null ? tx.getMemberPackage().getMember().getFullName() : "-";
        } else if (tx.getSale() != null) {
            type = "Bán lẻ";
            desc = "Hóa đơn #" + tx.getSale().getId();
            buyer = tx.getSale().getMember() != null ? tx.getSale().getMember().getFullName() : "Khách vãng lai";
        }

        return TransactionReportDTO.builder()
                .id(tx.getId())
                .transactionDate(tx.getTransactionDate() != null ? tx.getTransactionDate() : java.time.OffsetDateTime.now())
                .amount(tx.getAmount() != null ? tx.getAmount() : java.math.BigDecimal.ZERO)
                .paymentMethod(tx.getPaymentMethod())
                .status(tx.getStatus())
                .createdByStaffName(tx.getCreatedBy() != null ? tx.getCreatedBy().getFullName() : "-")
                .transactionType(type)
                .description(desc)
                .buyerName(buyer)
                .build();
    }
}
