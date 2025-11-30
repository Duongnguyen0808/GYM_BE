package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.dtos.MemberResponseDTO;
import com.gym.service.gymmanagementservice.dtos.SaleRequestDTO;
import com.gym.service.gymmanagementservice.models.Product;
import com.gym.service.gymmanagementservice.models.Promotion;
import com.gym.service.gymmanagementservice.services.MemberService;
import com.gym.service.gymmanagementservice.services.ProductService;
import com.gym.service.gymmanagementservice.services.PromotionService;
import com.gym.service.gymmanagementservice.services.SaleService;
import com.gym.service.gymmanagementservice.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/pos")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class SaleWebController {

    private final ProductService productService;
    private final MemberService memberService;
    private final SaleService saleService;
    private final PaymentService paymentService;
    private final PromotionService promotionService;

    @GetMapping
    public String getPosPage(Model model) {
        List<Product> products = productService.getAllProducts().stream()
                .filter(Product::isActive)
                .collect(Collectors.toList());

        List<MemberResponseDTO> members = memberService.getAllMembers();

        // Lấy thông tin khuyến mãi cho từng sản phẩm và tính giá đã giảm
        Map<Long, Promotion> productPromotions = new HashMap<>();
        Map<Long, java.math.BigDecimal> productDiscountedPrices = new HashMap<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (Product product : products) {
            java.math.BigDecimal originalPrice = product.getPrice();
            promotionService.getActivePromotionForProduct(product.getId(), now)
                    .ifPresent(promo -> {
                        productPromotions.put(product.getId(), promo);
                        // Tính giá đã giảm
                        java.math.BigDecimal discountedPrice = promotionService.applyDiscount(originalPrice, promo);
                        productDiscountedPrices.put(product.getId(), discountedPrice);
                    });
        }

        if (!model.containsAttribute("saleRequest")) {
            model.addAttribute("saleRequest", new SaleRequestDTO());
        }

        model.addAttribute("products", products);
        model.addAttribute("productPromotions", productPromotions);
        model.addAttribute("productDiscountedPrices", productDiscountedPrices);
        model.addAttribute("members", members);
        model.addAttribute("pageTitle", "Bán hàng tại quầy");
        model.addAttribute("contentView", "pos");
        model.addAttribute("activePage", "pos"); // <-- BÁO ACTIVE

        return "fragments/layout";
    }

    @PostMapping
    public String processPosSale(HttpServletRequest httpRequest,
                                 @Valid @ModelAttribute("saleRequest") SaleRequestDTO request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        if (bindingResult.hasErrors() || request.getItems() == null || request.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng trống hoặc chưa chọn hình thức thanh toán.");
            return "redirect:/pos";
        }

        try {
            if (request.getPaymentMethod() == com.gym.service.gymmanagementservice.models.PaymentMethod.CASH) {
                saleService.createPosSale(request);
                redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công!");
                return "redirect:/pos";
            } else if (request.getPaymentMethod() == com.gym.service.gymmanagementservice.models.PaymentMethod.VN_PAY) {
                // Thanh toán qua VNPay từ Admin web
                com.gym.service.gymmanagementservice.models.Sale pendingSale = saleService.createPendingSale(request);
                String vnpUrl = paymentService.createSalePaymentUrl(httpRequest, pendingSale.getId(), "admin");
                return "redirect:" + vnpUrl;
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Hình thức thanh toán không hỗ trợ.");
                return "redirect:/pos";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            redirectAttributes.addFlashAttribute("saleRequest", request);
            return "redirect:/pos";
        }
    }
}