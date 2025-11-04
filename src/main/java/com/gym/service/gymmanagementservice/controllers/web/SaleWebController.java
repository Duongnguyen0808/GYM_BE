package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.dtos.MemberResponseDTO;
import com.gym.service.gymmanagementservice.dtos.SaleRequestDTO;
import com.gym.service.gymmanagementservice.models.Product;
import com.gym.service.gymmanagementservice.services.MemberService;
import com.gym.service.gymmanagementservice.services.ProductService;
import com.gym.service.gymmanagementservice.services.SaleService;
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

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/pos")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class SaleWebController {

    private final ProductService productService;
    private final MemberService memberService;
    private final SaleService saleService;

    @GetMapping
    public String getPosPage(Model model) {
        List<Product> products = productService.getAllProducts().stream()
                .filter(Product::isActive)
                .collect(Collectors.toList());

        List<MemberResponseDTO> members = memberService.getAllMembers();

        if (!model.containsAttribute("saleRequest")) {
            model.addAttribute("saleRequest", new SaleRequestDTO());
        }

        model.addAttribute("products", products);
        model.addAttribute("members", members);
        model.addAttribute("pageTitle", "Bán hàng tại quầy (POS)");
        model.addAttribute("contentView", "pos");
        model.addAttribute("activePage", "pos"); // <-- BÁO ACTIVE

        return "fragments/layout";
    }

    @PostMapping
    public String processPosSale(@Valid @ModelAttribute("saleRequest") SaleRequestDTO request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        if (bindingResult.hasErrors() || request.getItems() == null || request.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng trống hoặc chưa chọn hình thức thanh toán.");
            return "redirect:/pos";
        }

        try {
            saleService.createPosSale(request);
            redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công!");
            return "redirect:/pos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            redirectAttributes.addFlashAttribute("saleRequest", request);
            return "redirect:/pos";
        }
    }
}