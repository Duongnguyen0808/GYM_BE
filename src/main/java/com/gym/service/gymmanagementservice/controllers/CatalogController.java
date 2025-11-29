package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.PackageResponseDTO;
import com.gym.service.gymmanagementservice.models.Product;
import com.gym.service.gymmanagementservice.services.PackageService;
import com.gym.service.gymmanagementservice.services.ProductService;
import com.gym.service.gymmanagementservice.services.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalog")
@Tag(name = "Catalog API", description = "API để người dùng đã đăng nhập xem các gói đang bán và sản phẩm")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class CatalogController {

    private final PackageService packageService;
    private final ProductService productService;
    private final PromotionService promotionService;

    @GetMapping("/packages")
    @Operation(summary = "Danh sách gói tập đang bán (chỉ gói đang active)")
    public List<PackageResponseDTO> getActivePackages() {
        OffsetDateTime now = OffsetDateTime.now();
        return packageService.getAllPackages()
                .stream()
                .filter(PackageResponseDTO::isActive)
                .map(pkg -> {
                    // Tính giá với promotion nếu có
                    java.math.BigDecimal originalPrice = pkg.getPrice();
                    java.math.BigDecimal discountedPrice = originalPrice;
                    
                    java.util.Optional<com.gym.service.gymmanagementservice.models.Promotion> promoOpt = 
                            promotionService.getActivePromotionForPackage(pkg.getId(), now);
                    if (promoOpt.isPresent()) {
                        discountedPrice = promotionService.applyDiscount(originalPrice, promoOpt.get());
                    }
                    
                    // Nếu có giảm giá, set originalPrice và discountedPrice
                    if (discountedPrice.compareTo(originalPrice) < 0) {
                        pkg.setOriginalPrice(originalPrice);
                        pkg.setDiscountedPrice(discountedPrice);
                    }
                    
                    return pkg;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/products")
    @Operation(summary = "Danh sách sản phẩm đang bán (chỉ sản phẩm đang active)")
    public List<Map<String, Object>> getActiveProducts() {
        OffsetDateTime now = OffsetDateTime.now();
        return productService.getAllProducts()
                .stream()
                .filter(Product::isActive)
                .map(product -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", product.getId());
                    result.put("name", product.getName());
                    result.put("price", product.getPrice());
                    result.put("stockQuantity", product.getStockQuantity());
                    result.put("isActive", product.isActive());
                    result.put("imageUrl", product.getHinhAnh());
                    result.put("hinh_anh", product.getHinhAnh());
                    
                    // Tính giá với promotion nếu có
                    java.math.BigDecimal originalPrice = product.getPrice();
                    java.math.BigDecimal discountedPrice = originalPrice;
                    
                    java.util.Optional<com.gym.service.gymmanagementservice.models.Promotion> promoOpt = 
                            promotionService.getActivePromotionForProduct(product.getId(), now);
                    if (promoOpt.isPresent()) {
                        discountedPrice = promotionService.applyDiscount(originalPrice, promoOpt.get());
                    }
                    
                    // Nếu có giảm giá, set originalPrice và discountedPrice
                    if (discountedPrice.compareTo(originalPrice) < 0) {
                        result.put("originalPrice", originalPrice);
                        result.put("discountedPrice", discountedPrice);
                    }
                    
                    return result;
                })
                .collect(Collectors.toList());
    }
}