package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.models.Sale;
import com.gym.service.gymmanagementservice.models.SaleDetail;
import com.gym.service.gymmanagementservice.models.Transaction;
import com.gym.service.gymmanagementservice.models.TransactionKind;
import com.gym.service.gymmanagementservice.repositories.CheckInLogRepository;
import com.gym.service.gymmanagementservice.repositories.SaleRepository;
import com.gym.service.gymmanagementservice.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final CheckInLogRepository checkInLogRepository;
    private final TransactionRepository transactionRepository;
    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public Map<String, Long> getCheckInsStats(OffsetDateTime start, OffsetDateTime end) {
        long total = checkInLogRepository.countByCheckInTimeBetween(start, end);
        long success = checkInLogRepository.countByCheckInTimeBetweenAndStatus(start, end, com.gym.service.gymmanagementservice.models.CheckInStatus.SUCCESS);
        long failed = total - success;
        Map<String, Long> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getSubscriptionStats(OffsetDateTime start, OffsetDateTime end) {
        Map<String, Long> result = new HashMap<>();
        result.put("new", transactionRepository.countByTransactionDateBetweenAndKind(start, end, TransactionKind.SUBSCRIPTION_NEW));
        result.put("renew", transactionRepository.countByTransactionDateBetweenAndKind(start, end, TransactionKind.SUBSCRIPTION_RENEW));
        result.put("upgrade", transactionRepository.countByTransactionDateBetweenAndKind(start, end, TransactionKind.SUBSCRIPTION_UPGRADE));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopProducts(OffsetDateTime start, OffsetDateTime end, int limit) {
        List<Sale> sales = saleRepository.findBySaleDateBetween(start, end);
        Map<Long, Integer> qtyByProductId = new HashMap<>();
        Map<Long, String> nameByProductId = new HashMap<>();

        for (Sale sale : sales) {
            for (SaleDetail d : sale.getSaleDetails()) {
                qtyByProductId.merge(d.getProduct().getId(), d.getQuantity(), Integer::sum);
                nameByProductId.putIfAbsent(d.getProduct().getId(), d.getProduct().getName());
            }
        }

        return qtyByProductId.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("productId", e.getKey());
                    m.put("productName", nameByProductId.get(e.getKey()));
                    m.put("quantity", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRevenueGrouped(OffsetDateTime start, OffsetDateTime end, String granularity) {
        List<Transaction> txs = transactionRepository.findByTransactionDateBetween(start, end);

        Map<String, BigDecimal> bucket = new LinkedHashMap<>();

        for (Transaction tx : txs) {
            LocalDate date = tx.getTransactionDate().toLocalDate();
            String key;
            if ("month".equalsIgnoreCase(granularity)) {
                YearMonth ym = YearMonth.from(date);
                key = ym.toString();
            } else { // week
                LocalDate weekStart = date.minusDays(date.getDayOfWeek().getValue() - 1);
                key = weekStart.toString();
            }

            BigDecimal val = bucket.getOrDefault(key, BigDecimal.ZERO);
            val = val.add(tx.getAmount());
            bucket.put(key, val);
        }

        return bucket.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("period", e.getKey());
                    m.put("amount", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }
}
