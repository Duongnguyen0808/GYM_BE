package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.config.VNPayConfig;
import com.gym.service.gymmanagementservice.dtos.*;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.*;
import com.gym.service.gymmanagementservice.utils.VNPayUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberCheckoutService {

    private final AuthenticationService authenticationService;
    private final MemberRepository memberRepository;
    private final GymPackageRepository gymPackageRepository;
    private final ProductRepository productRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final SaleRepository saleRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;
    private final SaleService saleService;
    private final PromotionService promotionService;
    private final VNPayConfig vnPayConfig;
    private final UserRepository userRepository;

    @Transactional
    public CheckoutResponseDTO processCheckout(HttpServletRequest request, CheckoutRequestDTO checkoutRequest) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Member member = currentUser.getMemberProfile();
        
        // Tự động tạo member profile nếu chưa có (giống MemberSubscriptionController)
        if (member == null) {
            member = Member.builder()
                    .fullName(currentUser.getFullName())
                    .phoneNumber(currentUser.getPhoneNumber())
                    .email(currentUser.getEmail())
                    .barcode(currentUser.getPhoneNumber())
                    .userAccount(currentUser)
                    .build();
            member = memberRepository.save(member);
            currentUser.setMemberProfile(member);
            currentUser.setRole(Role.MEMBER);
            userRepository.save(currentUser);
        }

        // Nếu thanh toán bằng VNPay
        if (checkoutRequest.getPaymentMethod() == PaymentMethod.VN_PAY) {
            return processVNPayCheckout(request, checkoutRequest, member, currentUser);
        } else {
            // Thanh toán trực tiếp (CASH, BANK_TRANSFER, CREDIT_CARD)
            return processDirectCheckout(checkoutRequest, member, currentUser);
        }
    }

    private CheckoutResponseDTO processVNPayCheckout(
            HttpServletRequest request,
            CheckoutRequestDTO checkoutRequest,
            Member member,
            User currentUser) {
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<MemberPackage> pendingSubscriptions = new ArrayList<>();
        Sale pendingSale = null;

        // Xử lý packages
        if (checkoutRequest.getPackages() != null && !checkoutRequest.getPackages().isEmpty()) {
            for (CheckoutRequestDTO.CheckoutPackageItemDTO item : checkoutRequest.getPackages()) {
                GymPackage gymPackage = gymPackageRepository.findById(item.getPackageId())
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập ID: " + item.getPackageId()));
                
                BigDecimal packagePrice = subscriptionService.calculateFinalPrice(member, gymPackage);
                totalAmount = totalAmount.add(packagePrice.multiply(BigDecimal.valueOf(item.getQuantity())));
                
                // Tạo pending subscription
                MemberPackage subscription = MemberPackage.builder()
                        .member(member)
                        .gymPackage(gymPackage)
                        .startDate(null)
                        .endDate(null)
                        .status(SubscriptionStatus.PENDING)
                        .build();
                pendingSubscriptions.add(memberPackageRepository.save(subscription));
            }
        }

        // Xử lý products
        if (checkoutRequest.getProducts() != null && !checkoutRequest.getProducts().isEmpty()) {
            SaleRequestDTO saleRequest = new SaleRequestDTO();
            saleRequest.setMemberId(member.getId());
            saleRequest.setPaymentMethod(null); // Chưa thanh toán
            
            List<SaleItemDTO> saleItems = new ArrayList<>();
            for (CheckoutRequestDTO.CheckoutProductItemDTO item : checkoutRequest.getProducts()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm ID: " + item.getProductId()));
                
                BigDecimal unitPrice = product.getPrice();
                OffsetDateTime now = OffsetDateTime.now();
                var promoOpt = promotionService.getActivePromotionForProduct(product.getId(), now);
                if (promoOpt.isPresent()) {
                    unitPrice = promotionService.applyDiscount(unitPrice, promoOpt.get());
                }
                
                totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
                
                SaleItemDTO saleItem = new SaleItemDTO();
                saleItem.setProductId(item.getProductId());
                saleItem.setQuantity(item.getQuantity());
                saleItems.add(saleItem);
            }
            
            saleRequest.setItems(saleItems);
            pendingSale = saleService.createPendingSale(saleRequest);
        }

        // Tạo transaction PENDING
        Transaction transaction = Transaction.builder()
                .amount(totalAmount)
                .paymentMethod(PaymentMethod.VN_PAY)
                .status(TransactionStatus.PENDING)
                .kind(TransactionKind.SUBSCRIPTION_NEW) // Có thể cần thêm kind mới cho checkout tổng hợp
                .transactionDate(OffsetDateTime.now())
                .createdBy(currentUser)
                .memberPackage(pendingSubscriptions.isEmpty() ? null : pendingSubscriptions.get(0))
                .sale(pendingSale)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Tạo VNPay URL
        String paymentUrl = VNPayUtil.createPaymentUrl(request, vnPayConfig, savedTransaction.getId(), totalAmount);

        return CheckoutResponseDTO.builder()
                .paymentUrl(paymentUrl)
                .success(true)
                .message("Đã tạo yêu cầu thanh toán VNPay")
                .build();
    }

    private CheckoutResponseDTO processDirectCheckout(
            CheckoutRequestDTO checkoutRequest,
            Member member,
            User currentUser) {
        
        // Xử lý packages
        if (checkoutRequest.getPackages() != null && !checkoutRequest.getPackages().isEmpty()) {
            for (CheckoutRequestDTO.CheckoutPackageItemDTO item : checkoutRequest.getPackages()) {
                for (int i = 0; i < item.getQuantity(); i++) {
                    SubscriptionRequestDTO subRequest = new SubscriptionRequestDTO();
                    subRequest.setMemberId(member.getId());
                    subRequest.setPackageId(item.getPackageId());
                    subRequest.setPaymentMethod(checkoutRequest.getPaymentMethod());
                    subscriptionService.createSubscription(subRequest);
                }
            }
        }

        // Xử lý products
        if (checkoutRequest.getProducts() != null && !checkoutRequest.getProducts().isEmpty()) {
            SaleRequestDTO saleRequest = new SaleRequestDTO();
            saleRequest.setMemberId(member.getId());
            saleRequest.setPaymentMethod(checkoutRequest.getPaymentMethod());
            
            List<SaleItemDTO> saleItems = new ArrayList<>();
            for (CheckoutRequestDTO.CheckoutProductItemDTO item : checkoutRequest.getProducts()) {
                SaleItemDTO saleItem = new SaleItemDTO();
                saleItem.setProductId(item.getProductId());
                saleItem.setQuantity(item.getQuantity());
                saleItems.add(saleItem);
            }
            
            saleRequest.setItems(saleItems);
            saleService.createPosSale(saleRequest);
        }

        return CheckoutResponseDTO.builder()
                .success(true)
                .message("Thanh toán thành công")
                .build();
    }
}

