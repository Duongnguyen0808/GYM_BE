package com.gym.service.gymmanagementservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_details")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaleDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thuộc về hóa đơn nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    // Sản phẩm nào được bán
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    // Giá tại thời điểm bán (để tránh ảnh hưởng khi giá sản phẩm thay đổi trong tương lai)
    @Column(name = "price_at_sale", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtSale;
}