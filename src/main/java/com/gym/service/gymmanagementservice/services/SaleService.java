package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.SaleItemDTO;
import com.gym.service.gymmanagementservice.dtos.SaleRequestDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final AuthenticationService authenticationService;
    private final TransactionRepository transactionRepository;

    /**
     * MỚI: Hàm private để xử lý logic trừ tồn kho.
     * Sẽ được gọi khi thanh toán (POS) hoặc khi IPN (VNPay) xác nhận.
     */
    @Transactional
    public void deductStockForSale(Sale sale) {
        for (SaleDetail detail : sale.getSaleDetails()) {
            Product product = detail.getProduct(); // Product đã được lock
            if (product.getStockQuantity() < detail.getQuantity()) {
                throw new IllegalStateException("Không đủ tồn kho (ID: " + product.getId() + ") cho sản phẩm: " + product.getName());
            }
            product.setStockQuantity(product.getStockQuantity() - detail.getQuantity());
            productRepository.save(product);
        }
    }

    /**
     * MỚI: Hàm private để xây dựng đối tượng Sale và SaleDetail
     * (Chưa lưu, chưa trừ kho, chưa tạo giao dịch)
     */
    private Sale buildSaleFromRequest(SaleRequestDTO request, User currentUser, SaleStatus status) {
        Member member = null;
        if (request.getMemberId() != null) {
            member = memberRepository.findById(request.getMemberId()).orElse(null);
        }

        Sale sale = Sale.builder()
                .user(currentUser)
                .member(member)
                .saleDate(OffsetDateTime.now())
                .saleDetails(new ArrayList<>())
                .status(status) // Set status
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SaleItemDTO item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Sản phẩm không tồn tại: " + item.getProductId()));

            // Chỉ kiểm tra, chưa trừ
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Không đủ tồn kho cho sản phẩm: " + product.getName());
            }

            SaleDetail detail = SaleDetail.builder()
                    .sale(sale)
                    .product(product)
                    .quantity(item.getQuantity())
                    .priceAtSale(product.getPrice())
                    .build();

            sale.getSaleDetails().add(detail);
            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }

        sale.setTotalAmount(totalAmount);
        return sale;
    }


    /**
     * SỬA: Luồng 1 - Tạo hóa đơn bán tại quầy (POS)
     * Giả định đã thanh toán (CASH, CREDIT_CARD tại quầy)
     */
    @Transactional
    public Sale createPosSale(SaleRequestDTO request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        if (request.getPaymentMethod() == null || request.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("Hình thức thanh toán tại quầy (POS) không hợp lệ.");
        }

        // 1. Build Sale
        Sale sale = buildSaleFromRequest(request, currentUser, SaleStatus.PAID); // Status PAID
        Sale savedSale = saleRepository.save(sale);

        // 2. Trừ tồn kho
        deductStockForSale(savedSale);

        // 3. Tạo Giao dịch (Transaction)
        Transaction transaction = Transaction.builder()
                .amount(savedSale.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.COMPLETED)
                .transactionDate(OffsetDateTime.now())
                .createdBy(currentUser)
                .sale(savedSale)
                .build();
        transactionRepository.save(transaction);

        return savedSale;
    }

    /**
     * MỚI: Luồng 2 - Khởi tạo hóa đơn (Chờ thanh toán online)
     * CHƯA trừ tồn kho, CHƯA tạo giao dịch.
     */
    @Transactional
    public Sale createPendingSale(SaleRequestDTO request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        // 1. Build Sale (chỉ kiểm tra tồn kho, chưa trừ)
        Sale sale = buildSaleFromRequest(request, currentUser, SaleStatus.PENDING_PAYMENT);

        // 2. Lưu Sale
        return saleRepository.save(sale);
    }
}