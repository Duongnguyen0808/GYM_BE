package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.ProductRequestDTO;
import com.gym.service.gymmanagementservice.models.Product;
import com.gym.service.gymmanagementservice.repositories.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    @Transactional
    public Product createProduct(ProductRequestDTO request) {
        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .isActive(true)
                .hinhAnh(request.getImageUrl())
                .build();
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * MỚI: Lấy thông tin 1 sản phẩm
     */
    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));
    }

    @Transactional
    public Product updateProduct(Long productId, ProductRequestDTO request) {
        Product product = getProductById(productId); // Sửa: Dùng hàm getProductById

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            product.setHinhAnh(request.getImageUrl());
        }

        return productRepository.save(product);
    }

    /**
     * MỚI: Ẩn hoặc Hiện sản phẩm (Ngừng bán / Bán lại)
     */
    @Transactional
    public void toggleProductStatus(Long productId) {
        Product product = getProductById(productId);
        product.setActive(!product.isActive()); // Đảo ngược trạng thái
        productRepository.save(product);
    }
}