package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.models.Promotion;
import com.gym.service.gymmanagementservice.models.PromotionTargetType;
import com.gym.service.gymmanagementservice.repositories.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public Optional<Promotion> getActivePromotionForProduct(Long productId, OffsetDateTime now) {
        var list = promotionRepository.findByTargetTypeAndTargetIdAndIsActiveIsTrueAndStartAtBeforeAndEndAtAfter(
                PromotionTargetType.PRODUCT, productId, now, now);
        return list.stream().findFirst();
    }

    public Optional<Promotion> getActivePromotionForPackage(Long packageId, OffsetDateTime now) {
        var list = promotionRepository.findByTargetTypeAndTargetIdAndIsActiveIsTrueAndStartAtBeforeAndEndAtAfter(
                PromotionTargetType.PACKAGE, packageId, now, now);
        return list.stream().findFirst();
    }
    
    /**
     * Overload method - tự động dùng thời gian hiện tại
     */
    public Optional<Promotion> getActivePromotionForPackage(Long packageId) {
        return getActivePromotionForPackage(packageId, OffsetDateTime.now());
    }

    public BigDecimal applyDiscount(BigDecimal originalPrice, Promotion promotion) {
        if (promotion == null) return originalPrice;
        BigDecimal percent = promotion.getDiscountPercent() == null ? BigDecimal.ZERO : promotion.getDiscountPercent();
        BigDecimal discount = originalPrice.multiply(percent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal result = originalPrice.subtract(discount);
        if (result.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    public java.util.List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    public Promotion createPromotion(com.gym.service.gymmanagementservice.dtos.PromotionRequestDTO request) {
        Promotion p = Promotion.builder()
                .name(request.getName())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .discountPercent(request.getDiscountPercent())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .isActive(true)
                .build();
        return promotionRepository.save(p);
    }

    public Promotion updatePromotion(Long id, com.gym.service.gymmanagementservice.dtos.PromotionRequestDTO request) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Không tìm thấy ưu đãi với ID: " + id));
        p.setName(request.getName());
        p.setTargetType(request.getTargetType());
        p.setTargetId(request.getTargetId());
        p.setDiscountPercent(request.getDiscountPercent());
        p.setStartAt(request.getStartAt());
        p.setEndAt(request.getEndAt());
        return promotionRepository.save(p);
    }

    public void deletePromotion(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy ưu đãi với ID: " + id);
        }
        promotionRepository.deleteById(id);
    }
}
