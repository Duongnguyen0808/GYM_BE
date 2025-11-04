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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String getPackagesPage(Model model) {
        List<PackageResponseDTO> packages = packageService.getAllPackages();
        model.addAttribute("packages", packages);
        model.addAttribute("pageTitle", "Quản lý Gói tập");
        model.addAttribute("contentView", "admin/packages");
        model.addAttribute("activePage", "adminPackages");
        return "fragments/layout";
    }
    @GetMapping("/packages/create")
    public String showCreatePackageForm(Model model) {
        model.addAttribute("packageRequest", new PackageRequestDTO());
        model.addAttribute("allPackageTypes", PackageType.values());
        model.addAttribute("pageTitle", "Tạo Gói tập mới");
        model.addAttribute("contentView", "admin/package-form");
        model.addAttribute("activePage", "adminPackages");
        return "fragments/layout";
    }
    @PostMapping("/packages/create")
    public String processCreatePackage(@Valid @ModelAttribute("packageRequest") PackageRequestDTO packageRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allPackageTypes", PackageType.values());
            model.addAttribute("pageTitle", "Tạo Gói tập mới");
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        }
        try {
            packageService.createPackage(packageRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo gói tập thành công!");
            return "redirect:/admin/packages";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("allPackageTypes", PackageType.values());
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
            packageRequest.setDurationDays(pkg.getDurationDays());
            packageRequest.setNumberOfSessions(pkg.getNumberOfSessions());
            packageRequest.setStartTimeLimit(pkg.getStartTimeLimit());
            packageRequest.setEndTimeLimit(pkg.getEndTimeLimit());
            model.addAttribute("packageRequest", packageRequest);
            model.addAttribute("packageId", packageId);
            model.addAttribute("allPackageTypes", PackageType.values());
            model.addAttribute("pageTitle", "Chỉnh sửa: " + pkg.getName());
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        } catch (Exception e) { return "redirect:/admin/packages"; }
    }
    @PostMapping("/packages/edit/{packageId}")
    public String processEditPackage(@PathVariable("packageId") Long packageId, @Valid @ModelAttribute("packageRequest") PackageRequestDTO packageRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("packageId", packageId);
            model.addAttribute("allPackageTypes", PackageType.values());
            model.addAttribute("pageTitle", "Chỉnh sửa Gói tập");
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        }
        try {
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

    // --- CÁC HÀM XỬ LÝ SẢN PHẨM (PRODUCTS) ---

    @GetMapping("/products")
    public String getProductsPage(Model model) {
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
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
    public String getSchedulesPage(Model model) {
        List<com.gym.service.gymmanagementservice.dtos.WorkScheduleResponseDTO> schedules = workScheduleService.getSchedules(
                java.time.OffsetDateTime.now().minusDays(30),
                java.time.OffsetDateTime.now().plusDays(30)
        );
        model.addAttribute("schedules", schedules);
        model.addAttribute("pageTitle", "Quản lý Lịch làm việc");
        model.addAttribute("contentView", "admin/schedules");
        model.addAttribute("activePage", "adminSchedules");
        return "fragments/layout";
    }

    @GetMapping("/schedules/create")
    public String showCreateScheduleForm(Model model) {
        List<UserResponseDTO> staffAndPt = staffService.getAllUsers().stream()
                .filter(user -> user.getRole() == Role.STAFF || user.getRole() == Role.PT)
                .collect(Collectors.toList());
        model.addAttribute("scheduleRequest", new com.gym.service.gymmanagementservice.dtos.WorkScheduleRequestDTO());
        model.addAttribute("staffAndPt", staffAndPt);
        model.addAttribute("pageTitle", "Xếp lịch làm việc mới");
        model.addAttribute("contentView", "admin/schedule-form");
        model.addAttribute("activePage", "adminSchedules");
        return "fragments/layout";
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
            model.addAttribute("pageTitle", "Xếp lịch làm việc mới");
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
            model.addAttribute("pageTitle", "Xếp lịch làm việc mới");
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
}