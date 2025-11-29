package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.dtos.AdminUpdateUserRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PackageRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PackageResponseDTO;
import com.gym.service.gymmanagementservice.dtos.ProductRequestDTO; // <-- IMPORT MỚI
import com.gym.service.gymmanagementservice.dtos.UserResponseDTO;
import com.gym.service.gymmanagementservice.models.PackageType;
import com.gym.service.gymmanagementservice.models.Product;
import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.services.*; // <-- SỬA IMPORT
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWebController {

    private final StaffService staffService;
    private final PackageService packageService;
    private final ProductService productService;
    private final WorkScheduleService workScheduleService;
    private final AuthenticationService authenticationService; // <-- THÊM DỊCH VỤ CÒN THIẾU
    private final CloudinaryService cloudinaryService;
    private final ReportService reportService;
    private final ReceiptService receiptService;
    private final StaffAttendanceService staffAttendanceService;
    private final PromotionService promotionService;
    private final com.gym.service.gymmanagementservice.repositories.PtSessionLogRepository ptSessionLogRepository;
    private final com.gym.service.gymmanagementservice.services.PtManagementService ptManagementService;
    private final com.gym.service.gymmanagementservice.services.CheckInService checkInService;
    private final com.gym.service.gymmanagementservice.services.MemberService memberService;

    // ... (Toàn bộ các hàm của User và Package giữ nguyên) ...
    @GetMapping("/users")
    public String getUsersPage(Model model) {
        List<UserResponseDTO> users = staffService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Quản lý Nhân viên");
        model.addAttribute("contentView", "admin/users");
        model.addAttribute("activePage", "adminUsers");
        return "fragments/layout";
    }
    @GetMapping("/users/edit/{userId}")
    public String showEditUserForm(@PathVariable("userId") Long userId, Model model) {
        try {
            UserResponseDTO user = staffService.getUserById(userId);
            if (user.getRole() == Role.MEMBER) { return "redirect:/admin/users"; }
            AdminUpdateUserRequestDTO userRequest = new AdminUpdateUserRequestDTO();
            userRequest.setFullName(user.getFullName());
            userRequest.setRole(user.getRole());
            userRequest.setLocked(user.isLocked());
            List<Role> staffRoles = Arrays.stream(Role.values()).filter(r -> r != Role.MEMBER).collect(Collectors.toList());
            model.addAttribute("userRequest", userRequest);
            model.addAttribute("userProfile", user);
            model.addAttribute("allRoles", staffRoles);
            model.addAttribute("pageTitle", "Chỉnh sửa: " + user.getFullName());
            model.addAttribute("contentView", "admin/user-edit");
            model.addAttribute("activePage", "adminUsers");
            return "fragments/layout";
        } catch (Exception e) { return "redirect:/admin/users"; }
    }
    @PostMapping("/users/edit/{userId}")
    public String processEditUser(@PathVariable("userId") Long userId, @Valid @ModelAttribute("userRequest") AdminUpdateUserRequestDTO userRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            UserResponseDTO user = staffService.getUserById(userId);
            List<Role> staffRoles = Arrays.stream(Role.values()).filter(r -> r != Role.MEMBER).collect(Collectors.toList());
            model.addAttribute("userProfile", user);
            model.addAttribute("allRoles", staffRoles);
            model.addAttribute("pageTitle", "Chỉnh sửa: " + user.getFullName());
            model.addAttribute("contentView", "admin/user-edit");
            model.addAttribute("activePage", "adminUsers");
            return "fragments/layout";
        }
        try {
            staffService.updateUserByAdmin(userId, userRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật nhân viên thành công!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/users/edit/" + userId;
        }
    }
    @PostMapping("/users/toggle-lock/{userId}")
    public String toggleUserLock(@PathVariable("userId") Long userId, RedirectAttributes redirectAttributes) {
        try {
            staffService.toggleUserLockStatus(userId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thay đổi trạng thái tài khoản.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
    @PostMapping("/users/delete/{userId}")
    public String deleteUser(@PathVariable("userId") Long userId, RedirectAttributes redirectAttributes) {
        try {
            staffService.deleteUserByAdmin(userId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa người dùng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
    @GetMapping("/users/create")
    public String showCreateUserForm(Model model) {
        List<Role> staffRoles = Arrays.stream(Role.values())
                .filter(r -> r != Role.MEMBER)
                .collect(Collectors.toList());
        model.addAttribute("userRequest", new com.gym.service.gymmanagementservice.dtos.AdminCreateUserRequestDTO());
        model.addAttribute("allRoles", staffRoles);
        model.addAttribute("pageTitle", "Tạo Nhân viên mới");
        model.addAttribute("contentView", "admin/user-create");
        model.addAttribute("activePage", "adminUsers");
        return "fragments/layout";
    }
    @PostMapping("/users/create")
    public String processCreateUser(@Valid @ModelAttribute("userRequest") com.gym.service.gymmanagementservice.dtos.AdminCreateUserRequestDTO userRequest,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            List<Role> staffRoles = Arrays.stream(Role.values())
                    .filter(r -> r != Role.MEMBER)
                    .collect(Collectors.toList());
            model.addAttribute("allRoles", staffRoles);
            model.addAttribute("pageTitle", "Tạo Nhân viên mới");
            model.addAttribute("contentView", "admin/user-create");
            model.addAttribute("activePage", "adminUsers");
            return "fragments/layout";
        }
        try {
            authenticationService.createStaffAccount(userRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản nhân viên thành công!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            List<Role> staffRoles = Arrays.stream(Role.values())
                    .filter(r -> r != Role.MEMBER)
                    .collect(Collectors.toList());
            model.addAttribute("allRoles", staffRoles);
            model.addAttribute("pageTitle", "Tạo Nhân viên mới");
            model.addAttribute("contentView", "admin/user-create");
            model.addAttribute("activePage", "adminUsers");
            return "fragments/layout";
        }
    }
    @GetMapping("/packages")
    public String getPackagesPage(@RequestParam(value = "q", required = false) String q,
                                  @RequestParam(value = "minPrice", required = false) java.math.BigDecimal minPrice,
                                  @RequestParam(value = "maxPrice", required = false) java.math.BigDecimal maxPrice,
                                  @RequestParam(value = "durationDays", required = false) Integer durationDays,
                                  @RequestParam(value = "type", required = false) String type,
                                  @RequestParam(value = "active", required = false) Boolean active,
                                  Model model) {
        String qParam = (q == null || q.isBlank()) ? null : q;
        String typeParam = (type == null || type.isBlank()) ? null : type;
        List<PackageResponseDTO> packages = packageService.searchPackages(qParam, minPrice, maxPrice, durationDays, typeParam, active);
        
        // Lấy thông tin khuyến mãi cho từng gói tập và tính giá đã giảm
        java.util.Map<Long, com.gym.service.gymmanagementservice.models.Promotion> packagePromotions = new java.util.HashMap<>();
        java.util.Map<Long, java.math.BigDecimal> packageDiscountedPrices = new java.util.HashMap<>();
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        for (PackageResponseDTO pkg : packages) {
            java.math.BigDecimal originalPrice = pkg.getPrice();
            promotionService.getActivePromotionForPackage(pkg.getId(), now)
                    .ifPresent(promo -> {
                        packagePromotions.put(pkg.getId(), promo);
                        // Tính giá đã giảm
                        java.math.BigDecimal discountedPrice = promotionService.applyDiscount(originalPrice, promo);
                        packageDiscountedPrices.put(pkg.getId(), discountedPrice);
                    });
        }
        
        model.addAttribute("packages", packages);
        model.addAttribute("packagePromotions", packagePromotions);
        model.addAttribute("packageDiscountedPrices", packageDiscountedPrices);
        model.addAttribute("q", q);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("durationDays", durationDays);
        model.addAttribute("type", type);
        model.addAttribute("active", active);
        model.addAttribute("allPackageTypes", com.gym.service.gymmanagementservice.models.PackageType.values());
        model.addAttribute("pageTitle", "Quản lý Gói tập");
        model.addAttribute("contentView", "admin/packages");
        model.addAttribute("activePage", "adminPackages");
        return "fragments/layout";
    }
    @GetMapping("/packages/create")
    public String showCreatePackageForm(Model model) {
        model.addAttribute("packageRequest", new PackageRequestDTO());
        model.addAttribute("allPackageTypes", PackageType.values());
        // Lấy danh sách PT để chọn khi tạo gói PT
        try {
            List<UserResponseDTO> allPts = staffService.getAllPts();
            model.addAttribute("allPts", allPts != null ? allPts : java.util.Collections.emptyList());
        } catch (Exception e) {
            // Nếu có lỗi khi lấy danh sách PT, vẫn cho phép tạo gói (không bắt buộc phải có PT)
            model.addAttribute("allPts", java.util.Collections.emptyList());
        }
        model.addAttribute("pageTitle", "Tạo Gói tập mới");
        model.addAttribute("contentView", "admin/package-form");
        model.addAttribute("activePage", "adminPackages");
        return "fragments/layout";
    }
    @PostMapping("/packages/create")
    public String processCreatePackage(@Valid @ModelAttribute("packageRequest") PackageRequestDTO packageRequest,
                                      @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allPackageTypes", PackageType.values());
            try {
                List<UserResponseDTO> allPts = staffService.getAllPts();
                model.addAttribute("allPts", allPts != null ? allPts : java.util.Collections.emptyList());
            } catch (Exception e) {
                model.addAttribute("allPts", java.util.Collections.emptyList());
            }
            model.addAttribute("pageTitle", "Tạo Gói tập mới");
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        }
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String url = cloudinaryService.upload(imageFile);
                packageRequest.setImageUrl(url);
            }
            packageService.createPackage(packageRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo gói tập thành công!");
            return "redirect:/admin/packages";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("allPackageTypes", PackageType.values());
            try {
                List<UserResponseDTO> allPts = staffService.getAllPts();
                model.addAttribute("allPts", allPts != null ? allPts : java.util.Collections.emptyList());
            } catch (Exception ex) {
                model.addAttribute("allPts", java.util.Collections.emptyList());
            }
            model.addAttribute("pageTitle", "Tạo Gói tập mới");
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        }
    }
    @GetMapping("/packages/edit/{packageId}")
    public String showEditPackageForm(@PathVariable("packageId") Long packageId, Model model) {
        try {
            PackageResponseDTO pkg = packageService.getPackageById(packageId);
            PackageRequestDTO packageRequest = new PackageRequestDTO();
            packageRequest.setName(pkg.getName());
            packageRequest.setDescription(pkg.getDescription());
            packageRequest.setPrice(pkg.getPrice());
            packageRequest.setPackageType(pkg.getPackageType());
            // Với GYM_ACCESS: Ưu tiên durationMonths, chỉ load durationDays nếu không có durationMonths
            if (pkg.getPackageType() == PackageType.GYM_ACCESS) {
                if (pkg.getDurationMonths() != null && pkg.getDurationMonths() > 0) {
                    packageRequest.setDurationMonths(pkg.getDurationMonths());
                    packageRequest.setDurationDays(null); // Clear durationDays khi có durationMonths
                } else {
                    packageRequest.setDurationDays(pkg.getDurationDays());
                    packageRequest.setDurationMonths(null); // Clear durationMonths khi dùng durationDays
                }
            } else {
                packageRequest.setDurationDays(pkg.getDurationDays());
                packageRequest.setDurationMonths(pkg.getDurationMonths());
            }
            packageRequest.setNumberOfSessions(pkg.getNumberOfSessions());
            packageRequest.setStartTimeLimit(pkg.getStartTimeLimit());
            packageRequest.setEndTimeLimit(pkg.getEndTimeLimit());
            packageRequest.setImageUrl(pkg.getImageUrl());
            packageRequest.setAssignedPtId(pkg.getAssignedPtId());
            packageRequest.setAllowedWeekdays(pkg.getAllowedWeekdays()); // Load allowedWeekdays khi edit
            model.addAttribute("packageRequest", packageRequest);
            model.addAttribute("packageId", packageId);
            model.addAttribute("allPackageTypes", PackageType.values());
            try {
                List<UserResponseDTO> allPts = staffService.getAllPts();
                model.addAttribute("allPts", allPts != null ? allPts : java.util.Collections.emptyList());
            } catch (Exception e) {
                model.addAttribute("allPts", java.util.Collections.emptyList());
            }
            model.addAttribute("pageTitle", "Chỉnh sửa: " + pkg.getName());
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        } catch (Exception e) { return "redirect:/admin/packages"; }
    }
    @PostMapping("/packages/edit/{packageId}")
    public String processEditPackage(@PathVariable("packageId") Long packageId,
                                    @Valid @ModelAttribute("packageRequest") PackageRequestDTO packageRequest,
                                    @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("packageId", packageId);
            model.addAttribute("allPackageTypes", PackageType.values());
            try {
                List<UserResponseDTO> allPts = staffService.getAllPts();
                model.addAttribute("allPts", allPts != null ? allPts : java.util.Collections.emptyList());
            } catch (Exception e) {
                model.addAttribute("allPts", java.util.Collections.emptyList());
            }
            model.addAttribute("pageTitle", "Chỉnh sửa Gói tập");
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        }
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String url = cloudinaryService.upload(imageFile);
                packageRequest.setImageUrl(url);
            }
            packageService.updatePackage(packageId, packageRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật gói tập thành công!");
            return "redirect:/admin/packages";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("packageId", packageId);
            model.addAttribute("allPackageTypes", PackageType.values());
            model.addAttribute("pageTitle", "Chỉnh sửa Gói tập");
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        }
    }
    @PostMapping("/packages/toggle/{packageId}")
    public String togglePackageStatus(@PathVariable("packageId") Long packageId, RedirectAttributes redirectAttributes) {
        try {
            packageService.togglePackageStatus(packageId);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/packages";
    }

    @PostMapping("/packages/delete/{packageId}")
    public String deletePackage(@PathVariable("packageId") Long packageId, RedirectAttributes redirectAttributes) {
        try {
            packageService.deletePackage(packageId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa gói tập.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/packages";
    }

    // --- CÁC HÀM XỬ LÝ SẢN PHẨM (PRODUCTS) ---

    @GetMapping("/products")
    public String getProductsPage(@RequestParam(value = "q", required = false) String q, Model model) {
        List<Product> products = productService.getAllProducts();
        if (q != null && !q.isBlank()) {
            String qq = q.toLowerCase();
            products = products.stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(qq))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // Lấy thông tin khuyến mãi cho từng sản phẩm và tính giá đã giảm
        java.util.Map<Long, com.gym.service.gymmanagementservice.models.Promotion> productPromotions = new java.util.HashMap<>();
        java.util.Map<Long, java.math.BigDecimal> productDiscountedPrices = new java.util.HashMap<>();
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
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
        
        model.addAttribute("products", products);
        model.addAttribute("productPromotions", productPromotions);
        model.addAttribute("productDiscountedPrices", productDiscountedPrices);
        model.addAttribute("q", q);
        model.addAttribute("pageTitle", "Quản lý Sản phẩm (POS)");
        model.addAttribute("contentView", "admin/products");
        model.addAttribute("activePage", "adminProducts");
        return "fragments/layout";
    }

    /**
     * MỚI: Hiển thị form TẠO MỚI sản phẩm
     */
    @GetMapping("/products/create")
    public String showCreateProductForm(Model model) {
        model.addAttribute("productRequest", new ProductRequestDTO());
        model.addAttribute("pageTitle", "Tạo Sản phẩm mới");
        model.addAttribute("contentView", "admin/product-form"); // Dùng file form mới
        model.addAttribute("activePage", "adminProducts");
        return "fragments/layout";
    }

    /**
     * MỚI: Xử lý TẠO MỚI sản phẩm
     */
    @PostMapping("/products/create")
    public String processCreateProduct(@Valid @ModelAttribute("productRequest") ProductRequestDTO productRequest,
                                       @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Tạo Sản phẩm mới");
            model.addAttribute("contentView", "admin/product-form");
            model.addAttribute("activePage", "adminProducts");
            return "fragments/layout";
        }
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String url = cloudinaryService.upload(imageFile);
                productRequest.setImageUrl(url);
            }
            productService.createProduct(productRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo sản phẩm thành công!");
            return "redirect:/admin/products";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("pageTitle", "Tạo Sản phẩm mới");
            model.addAttribute("contentView", "admin/product-form");
            model.addAttribute("activePage", "adminProducts");
            return "fragments/layout";
        }
    }

    /**
     * MỚI: Hiển thị form CHỈNH SỬA sản phẩm
     */
    @GetMapping("/products/edit/{productId}")
    public String showEditProductForm(@PathVariable("productId") Long productId, Model model) {
        try {
            Product product = productService.getProductById(productId);

            // Chuyển từ Model sang DTO để điền form
            ProductRequestDTO productRequest = new ProductRequestDTO();
            productRequest.setName(product.getName());
            productRequest.setPrice(product.getPrice());
            productRequest.setStockQuantity(product.getStockQuantity());
            productRequest.setImageUrl(product.getHinhAnh());

            model.addAttribute("productRequest", productRequest);
            model.addAttribute("productId", productId); // Để biết là form Sửa
            model.addAttribute("pageTitle", "Chỉnh sửa: " + product.getName());
            model.addAttribute("contentView", "admin/product-form");
            model.addAttribute("activePage", "adminProducts");
            return "fragments/layout";
        } catch (Exception e) {
            return "redirect:/admin/products";
        }
    }

    /**
     * MỚI: Xử lý CHỈNH SỬA sản phẩm
     */
    @PostMapping("/products/edit/{productId}")
    public String processEditProduct(@PathVariable("productId") Long productId,
                                     @Valid @ModelAttribute("productRequest") ProductRequestDTO productRequest,
                                     @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", productId);
            model.addAttribute("pageTitle", "Chỉnh sửa Sản phẩm");
            model.addAttribute("contentView", "admin/product-form");
            model.addAttribute("activePage", "adminProducts");
            return "fragments/layout";
        }
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String url = cloudinaryService.upload(imageFile);
                productRequest.setImageUrl(url);
            }
            productService.updateProduct(productId, productRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
            return "redirect:/admin/products";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("productId", productId);
            model.addAttribute("pageTitle", "Chỉnh sửa Sản phẩm");
            model.addAttribute("contentView", "admin/product-form");
            model.addAttribute("activePage", "adminProducts");
            return "fragments/layout";
        }
    }

    /**
     * MỚI: Xử lý Ngừng/Mở bán (Toggle Status)
     */
    @PostMapping("/products/toggle/{productId}")
    public String toggleProductStatus(@PathVariable("productId") Long productId, RedirectAttributes redirectAttributes) {
        try {
            productService.toggleProductStatus(productId);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái sản phẩm thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // --- SCHEDULE MANAGEMENT ---

    @GetMapping("/schedules")
    public String getSchedulesPage(@RequestParam(value = "ptId", required = false) Long ptId, Model model) {
        // Lấy danh sách tất cả PT
        List<UserResponseDTO> allPts = staffService.getAllUsers().stream()
                .filter(user -> user.getRole() == Role.PT)
                .collect(Collectors.toList());
        
        model.addAttribute("allPts", allPts);
        
        // Nếu có chọn PT, lấy lịch tập của PT đó
        if (ptId != null) {
            List<com.gym.service.gymmanagementservice.models.PtSessionLog> ptSessions = 
                ptSessionLogRepository.findByPtUserIdWithDetails(ptId);
            model.addAttribute("ptSessions", ptSessions);
            model.addAttribute("selectedPtId", ptId);
            
            // Tìm thông tin PT
            UserResponseDTO selectedPt = allPts.stream()
                    .filter(pt -> pt.getId().equals(ptId))
                    .findFirst()
                    .orElse(null);
            model.addAttribute("selectedPt", selectedPt);
        }
        
        model.addAttribute("pageTitle", "Quản lý Lịch tập PT");
        model.addAttribute("contentView", "admin/schedules");
        model.addAttribute("activePage", "adminSchedules");
        return "fragments/layout";
    }

    // --- PT MANAGEMENT ---

    @GetMapping("/pts")
    public String getPtsManagementPage(Model model) {
        // Lấy danh sách tất cả PT
        List<UserResponseDTO> allPts = staffService.getAllUsers().stream()
                .filter(user -> user.getRole() == Role.PT)
                .collect(Collectors.toList());
        
        // Lấy thống kê cho từng PT
        List<java.util.Map<String, Object>> ptsWithStats = ptManagementService.getAllPtsWithStats(allPts);
        model.addAttribute("ptsWithStats", ptsWithStats);
        
        model.addAttribute("pageTitle", "Quản lý PT");
        model.addAttribute("contentView", "admin/pts");
        model.addAttribute("activePage", "adminPts");
        return "fragments/layout";
    }

    @GetMapping("/pts/{ptId}/stats")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> getPtStatisticsApi(@PathVariable("ptId") Long ptId) {
        try {
            java.util.Map<String, Object> ptStats = ptManagementService.getPtStatistics(ptId);
            
            // Lấy thông tin PT
            UserResponseDTO selectedPt = staffService.getAllUsers().stream()
                    .filter(user -> user.getRole() == Role.PT && user.getId().equals(ptId))
                    .findFirst()
                    .orElse(null);
            
            if (selectedPt == null) {
                return ResponseEntity.notFound().build();
            }
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("pt", selectedPt);
            response.put("stats", ptStats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/schedules/create")
    public String showCreateScheduleForm(Model model) {
        List<UserResponseDTO> staffAndPt = staffService.getAllUsers().stream()
                .filter(user -> user.getRole() == Role.STAFF || user.getRole() == Role.PT)
                .collect(Collectors.toList());
        model.addAttribute("scheduleRequest", new com.gym.service.gymmanagementservice.dtos.WorkScheduleRequestDTO());
        model.addAttribute("staffAndPt", staffAndPt);
        model.addAttribute("pageTitle", "Xếp lịch lớp/nhóm");
        model.addAttribute("contentView", "admin/schedule-form");
        model.addAttribute("activePage", "adminSchedules");
        return "fragments/layout";
    }

    @PostMapping("/schedules/copy-week")
    public String copyWeek(@RequestParam("from") String from,
                           @RequestParam("to") String to,
                           RedirectAttributes redirectAttributes) {
        try {
            java.time.LocalDate f = java.time.LocalDate.parse(from);
            java.time.LocalDate t = java.time.LocalDate.parse(to);
            workScheduleService.copyWeek(f, t);
            redirectAttributes.addFlashAttribute("successMessage", "Đã sao chép lịch tuần.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/schedules";
    }

    @PostMapping("/schedules/create")
    public String processCreateSchedule(@Valid @ModelAttribute("scheduleRequest") com.gym.service.gymmanagementservice.dtos.WorkScheduleRequestDTO scheduleRequest,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        if (bindingResult.hasErrors()) {
            List<UserResponseDTO> staffAndPt = staffService.getAllUsers().stream()
                    .filter(user -> user.getRole() == Role.STAFF || user.getRole() == Role.PT)
                    .collect(Collectors.toList());
            model.addAttribute("staffAndPt", staffAndPt);
            model.addAttribute("pageTitle", "Xếp lịch lớp/nhóm");
            model.addAttribute("contentView", "admin/schedule-form");
            model.addAttribute("activePage", "adminSchedules");
            return "fragments/layout";
        }
        try {
            workScheduleService.createSchedule(scheduleRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Xếp lịch thành công!");
            return "redirect:/admin/schedules";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            List<UserResponseDTO> staffAndPt = staffService.getAllUsers().stream()
                    .filter(user -> user.getRole() == Role.STAFF || user.getRole() == Role.PT)
                    .collect(Collectors.toList());
            model.addAttribute("staffAndPt", staffAndPt);
            model.addAttribute("pageTitle", "Xếp lịch lớp/nhóm");
            model.addAttribute("contentView", "admin/schedule-form");
            model.addAttribute("activePage", "adminSchedules");
            return "fragments/layout";
        }
    }

    @PostMapping("/schedules/delete/{scheduleId}")
    public String deleteSchedule(@PathVariable("scheduleId") Long scheduleId, RedirectAttributes redirectAttributes) {
        try {
            workScheduleService.deleteSchedule(scheduleId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa lịch làm việc.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/schedules";
    }

    @GetMapping("/reports/sales")
    public String getSalesReportPage(
            Model model,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "method", required = false) com.gym.service.gymmanagementservice.models.PaymentMethod method,
            @RequestParam(value = "status", required = false) com.gym.service.gymmanagementservice.models.TransactionStatus status
    ) {
        java.util.List<com.gym.service.gymmanagementservice.dtos.TransactionReportDTO> all = reportService.getFullTransactionReport();

        java.time.OffsetDateTime startDt = null;
        java.time.OffsetDateTime endDt = null;
        if (start != null && !start.isBlank()) {
            try { startDt = java.time.LocalDate.parse(start).atStartOfDay(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime(); } catch (Exception ignored) {}
        }
        if (end != null && !end.isBlank()) {
            try { endDt = java.time.LocalDate.parse(end).plusDays(1).atStartOfDay(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime(); } catch (Exception ignored) {}
        }

        final java.time.OffsetDateTime startDtLocal = startDt;
        final java.time.OffsetDateTime endDtLocal = endDt;

        java.util.List<com.gym.service.gymmanagementservice.dtos.TransactionReportDTO> reportData = all.stream()
                .filter(tx -> {
                    if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) return true;
                    if ("POS".equalsIgnoreCase(type)) return "Bán lẻ".equals(tx.getTransactionType());
                    if ("SUBSCRIPTION".equalsIgnoreCase(type)) return "Gói tập".equals(tx.getTransactionType());
                    return true;
                })
                .filter(tx -> method == null || tx.getPaymentMethod() == method)
                .filter(tx -> status == null || tx.getStatus() == status)
                .filter(tx -> {
                    if (startDtLocal != null && tx.getTransactionDate().isBefore(startDtLocal)) return false;
                    if (endDtLocal != null && !tx.getTransactionDate().isBefore(endDtLocal)) return false;
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

        java.math.BigDecimal totalRevenue = reportData.stream()
                .filter(tx -> tx.getStatus() == com.gym.service.gymmanagementservice.models.TransactionStatus.COMPLETED)
                .map(com.gym.service.gymmanagementservice.dtos.TransactionReportDTO::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("reportData", reportData);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("type", type);
        model.addAttribute("method", method);
        model.addAttribute("status", status);
        model.addAttribute("pageTitle", "Báo cáo Doanh thu");
        model.addAttribute("contentView", "admin/sales-report");
        model.addAttribute("activePage", "adminReports");
        return "fragments/layout";
    }

    // --- RECEIPTS ---
    @GetMapping("/receipts/transaction/{id}")
    public org.springframework.http.ResponseEntity<byte[]> downloadTransactionReceipt(@PathVariable("id") Long id) {
        try {
            byte[] pdf = receiptService.generateTransactionReceipt(id);
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=receipt-transaction-" + id + ".pdf")
                    .body(pdf);
        } catch (Exception e) {
            byte[] pdf = receiptService.generateErrorReceipt("BIEN NHAN", "Khong the xuat hoa don: " + e.getMessage());
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=receipt-transaction-" + id + ".pdf")
                    .body(pdf);
        }
    }
    @GetMapping("/receipts/sale/{id}")
    public org.springframework.http.ResponseEntity<byte[]> downloadSaleReceipt(@PathVariable("id") Long id) {
        try {
            byte[] pdf = receiptService.generateSaleReceipt(id);
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=receipt-sale-" + id + ".pdf")
                    .body(pdf);
        } catch (Exception e) {
            byte[] pdf = receiptService.generateErrorReceipt("BIEN NHAN", "Khong the xuat hoa don: " + e.getMessage());
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=receipt-sale-" + id + ".pdf")
                    .body(pdf);
        }
    }

    @GetMapping("/promotions")
    public String getPromotionsPage(Model model) {
        java.util.List<com.gym.service.gymmanagementservice.models.Promotion> promotions = promotionService.getAllPromotions();
        
        // Tạo map để lưu tên sản phẩm/gói tập cho mỗi promotion
        java.util.Map<Long, String> targetNames = new java.util.HashMap<>();
        for (com.gym.service.gymmanagementservice.models.Promotion p : promotions) {
            try {
                if (p.getTargetType() == com.gym.service.gymmanagementservice.models.PromotionTargetType.PRODUCT) {
                    Product product = productService.getProductById(p.getTargetId());
                    targetNames.put(p.getId(), product.getName());
                } else if (p.getTargetType() == com.gym.service.gymmanagementservice.models.PromotionTargetType.PACKAGE) {
                    PackageResponseDTO pkg = packageService.getPackageById(p.getTargetId());
                    targetNames.put(p.getId(), pkg.getName());
                }
            } catch (Exception e) {
                targetNames.put(p.getId(), "Không tìm thấy");
            }
        }
        
        model.addAttribute("promotions", promotions);
        model.addAttribute("targetNames", targetNames);
        model.addAttribute("pageTitle", "Ưu đãi/Khuyến mại");
        model.addAttribute("contentView", "admin/promotions");
        model.addAttribute("activePage", "adminPromotions");
        return "fragments/layout";
    }

    @GetMapping("/promotions/create")
    public String showCreatePromotionForm(Model model) {
        java.util.List<PackageResponseDTO> packages = packageService.getAllPackages();
        java.util.List<Product> products = productService.getAllProducts();
        model.addAttribute("packages", packages);
        model.addAttribute("products", products);
        model.addAttribute("pageTitle", "Tạo Ưu đãi/Khuyến mại");
        model.addAttribute("contentView", "admin/promotion-form");
        model.addAttribute("activePage", "adminPromotions");
        return "fragments/layout";
    }

    @PostMapping("/promotions/create")
    public String processCreatePromotion(
            @RequestParam("name") String name,
            @RequestParam("targetType") com.gym.service.gymmanagementservice.models.PromotionTargetType targetType,
            @RequestParam("targetId") Long targetId,
            @RequestParam("discountPercent") java.math.BigDecimal discountPercent,
            @RequestParam("start") String start,
            @RequestParam("end") String end,
            RedirectAttributes redirectAttributes
    ) {
        try {
            java.time.LocalDateTime s = java.time.LocalDateTime.parse(start);
            java.time.LocalDateTime e = java.time.LocalDateTime.parse(end);
            com.gym.service.gymmanagementservice.dtos.PromotionRequestDTO req = new com.gym.service.gymmanagementservice.dtos.PromotionRequestDTO();
            req.setName(name);
            req.setTargetType(targetType);
            req.setTargetId(targetId);
            req.setDiscountPercent(discountPercent);
            req.setStartAt(s.atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime());
            req.setEndAt(e.atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime());
            promotionService.createPromotion(req);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo ưu đãi thành công!");
            return "redirect:/admin/promotions";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
            return "redirect:/admin/promotions/create";
        }
    }

    @GetMapping("/promotions/edit/{id}")
    public String showEditPromotionForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            java.util.List<PackageResponseDTO> packages = packageService.getAllPackages();
            java.util.List<Product> products = productService.getAllProducts();
            com.gym.service.gymmanagementservice.models.Promotion p = promotionService.getAllPromotions().stream().filter(pr -> pr.getId().equals(id)).findFirst().orElseThrow();
            model.addAttribute("packages", packages);
            model.addAttribute("products", products);
            model.addAttribute("promotion", p);
            model.addAttribute("pageTitle", "Sửa Ưu đãi/Khuyến mại");
            model.addAttribute("contentView", "admin/promotion-form-edit");
            model.addAttribute("activePage", "adminPromotions");
            return "fragments/layout";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
            return "redirect:/admin/promotions";
        }
    }

    @PostMapping("/promotions/edit/{id}")
    public String processEditPromotion(@PathVariable("id") Long id,
                                       @RequestParam("name") String name,
                                       @RequestParam("targetType") com.gym.service.gymmanagementservice.models.PromotionTargetType targetType,
                                       @RequestParam("targetId") Long targetId,
                                       @RequestParam("discountPercent") java.math.BigDecimal discountPercent,
                                       @RequestParam("start") String start,
                                       @RequestParam("end") String end,
                                       RedirectAttributes redirectAttributes) {
        try {
            java.time.LocalDateTime s = java.time.LocalDateTime.parse(start);
            java.time.LocalDateTime e = java.time.LocalDateTime.parse(end);
            com.gym.service.gymmanagementservice.dtos.PromotionRequestDTO req = new com.gym.service.gymmanagementservice.dtos.PromotionRequestDTO();
            req.setName(name);
            req.setTargetType(targetType);
            req.setTargetId(targetId);
            req.setDiscountPercent(discountPercent);
            req.setStartAt(s.atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime());
            req.setEndAt(e.atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime());
            promotionService.updatePromotion(id, req);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật ưu đãi.");
            return "redirect:/admin/promotions";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
            return "redirect:/admin/promotions";
        }
    }

    @PostMapping("/promotions/delete/{id}")
    public String deletePromotion(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.deletePromotion(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa ưu đãi.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    @GetMapping("/check-in-logs")
    public String getCheckInLogsPage(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Long packageId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {
        try {
            java.time.OffsetDateTime start = null;
            java.time.OffsetDateTime end = null;
            
            if (startDate != null && !startDate.isEmpty()) {
                start = java.time.LocalDate.parse(startDate).atStartOfDay()
                        .atOffset(java.time.ZoneOffset.of("+07:00"));
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59)
                        .atOffset(java.time.ZoneOffset.of("+07:00"));
            }
            
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            java.util.List<com.gym.service.gymmanagementservice.models.CheckInLog> logs = 
                    checkInService.getCheckInLogs(memberId, packageId, start, end, pageable);
            
            java.util.List<com.gym.service.gymmanagementservice.dtos.CheckInLogResponseDTO> logDTOs = 
                    logs.stream()
                            .map(com.gym.service.gymmanagementservice.dtos.CheckInLogResponseDTO::fromCheckInLog)
                            .collect(java.util.stream.Collectors.toList());
            
            // Get all members for filter dropdown
            java.util.List<com.gym.service.gymmanagementservice.dtos.MemberResponseDTO> allMembers = 
                    memberService.getAllMembers();
            
            model.addAttribute("logs", logDTOs);
            model.addAttribute("members", allMembers);
            model.addAttribute("memberId", memberId);
            model.addAttribute("packageId", packageId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("pageTitle", "Lịch sử Check-in/Check-out");
            model.addAttribute("contentView", "admin/check-in-logs");
            model.addAttribute("activePage", "checkInLogs");
            
            return "fragments/layout";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi: " + e.getMessage());
            model.addAttribute("logs", java.util.Collections.emptyList());
            model.addAttribute("members", java.util.Collections.emptyList());
            model.addAttribute("pageTitle", "Lịch sử Check-in/Check-out");
            model.addAttribute("contentView", "admin/check-in-logs");
            model.addAttribute("activePage", "checkInLogs");
            return "fragments/layout";
        }
    }
}
