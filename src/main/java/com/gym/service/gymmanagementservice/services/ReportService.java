package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.TransactionReportDTO;
import com.gym.service.gymmanagementservice.models.Transaction;
import com.gym.service.gymmanagementservice.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort; // <-- IMPORT MỚI
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;

    /**
     * Lấy tất cả các giao dịch (để đơn giản)
     * (Thực tế nên lọc theo ngày và phân trang)
     */
    @Transactional(readOnly = true) // Chỉ đọc
    public List<TransactionReportDTO> getFullTransactionReport() {

        // Lấy tất cả, sắp xếp theo ngày mới nhất
        List<Transaction> transactions = transactionRepository.findAll(
                Sort.by(Sort.Direction.DESC, "transactionDate")
        );

        return transactions.stream()
                .map(TransactionReportDTO::fromTransaction)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionReportDTO> getMemberTimeline(Long memberId) {
        List<Transaction> transactions = transactionRepository.findAll(
                Sort.by(Sort.Direction.DESC, "transactionDate")
        );
        return transactions.stream()
                .filter(tx ->
                        (tx.getMemberPackage() != null && tx.getMemberPackage().getMember() != null && tx.getMemberPackage().getMember().getId().equals(memberId))
                                || (tx.getSale() != null && tx.getSale().getMember() != null && tx.getSale().getMember().getId().equals(memberId))
                )
                .map(TransactionReportDTO::fromTransaction)
                .collect(Collectors.toList());
    }
}
