package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.dtos.*;
import com.gym.service.gymmanagementservice.services.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/members")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class MemberWebController {

    private final MemberService memberService;
    private final SubscriptionService subscriptionService;
    private final PackageService packageService;
    private final StaffService staffService;
    private final PtSessionService ptSessionService;
    private final CloudinaryService cloudinaryService;
    private final ReportService reportService;
    private final com.gym.service.gymmanagementservice.services.CheckInService checkInService;
    private final PaymentService paymentService;

    @GetMapping
    public String getMembersPage(@RequestParam(value = "q", required = false) String q, Model model) {
        List<MemberResponseDTO> members = memberService.getAllMembers();
        if (q != null && !q.isBlank()) {
            String qq = q.toLowerCase();
            members = members.stream()
                    .filter(m -> (m.getFullName() != null && m.getFullName().toLowerCase().contains(qq))
                            || (m.getPhoneNumber() != null && m.getPhoneNumber().toLowerCase().contains(qq))
                            || (m.getEmail() != null && m.getEmail().toLowerCase().contains(qq)))
                    .collect(java.util.stream.Collectors.toList());
        }
        model.addAttribute("members", members);
        model.addAttribute("q", q);
        model.addAttribute("pageTitle", "Quản lý Hội viên");
        model.addAttribute("contentView", "members");
        model.addAttribute("activePage", "members"); // <-- BÁO ACTIVE
        return "fragments/layout";
    }

    @GetMapping("/create")
    public String showCreateMemberForm(Model model) {
        model.addAttribute("memberRequest", new MemberRequestDTO());
        model.addAttribute("pageTitle", "Tạo Hội viên mới");
        model.addAttribute("contentView", "member-form");
        model.addAttribute("activePage", "members"); // <-- BÁO ACTIVE
        return "fragments/layout";
    }

    @PostMapping("/create")
    public String processCreateMember(@Valid @ModelAttribute("memberRequest") MemberRequestDTO memberRequest,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Tạo Hội viên mới");
            model.addAttribute("contentView", "member-form");
            model.addAttribute("activePage", "members"); // <-- BÁO ACTIVE
            return "fragments/layout";
        }
        try {
            memberService.createMember(memberRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo hội viên thành công!");
            return "redirect:/members";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("pageTitle", "Tạo Hội viên mới");
            model.addAttribute("contentView", "member-form");
            model.addAttribute("activePage", "members"); // <-- BÁO ACTIVE
            return "fragments/layout";
        }
    }

    @GetMapping("/edit/{memberId}")
    public String showEditMemberForm(@PathVariable("memberId") Long memberId, Model model) {
        try {
            MemberResponseDTO member = memberService.getMemberById(memberId);
            MemberRequestDTO memberRequest = new MemberRequestDTO();
            memberRequest.setFullName(member.getFullName());
            memberRequest.setPhoneNumber(member.getPhoneNumber());
            memberRequest.setEmail(member.getEmail());
            memberRequest.setBirthDate(member.getBirthDate());
            memberRequest.setAddress(member.getAddress());
            model.addAttribute("memberRequest", memberRequest);
            model.addAttribute("memberId", memberId);
            model.addAttribute("pageTitle", "Chỉnh sửa: " + member.getFullName());
            model.addAttribute("contentView", "member-form");
            model.addAttribute("activePage", "members"); // <-- BÁO ACTIVE
            return "fragments/layout";
        } catch (Exception e) { return "redirect:/members"; }
    }

    @PostMapping("/edit/{memberId}")
    public String processEditMember(@PathVariable("memberId") Long memberId,
                                    @Valid @ModelAttribute("memberRequest") MemberRequestDTO memberRequest,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    Model model,
                                    @org.springframework.web.bind.annotation.RequestParam(value = "avatarFile", required = false) org.springframework.web.multipart.MultipartFile avatarFile) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("memberId", memberId);
            model.addAttribute("pageTitle", "Chỉnh sửa Hội viên");
            model.addAttribute("contentView", "member-form");
            model.addAttribute("activePage", "members"); // <-- BÁO ACTIVE
            return "fragments/layout";
        }
        try {
            String avatarUrl = null;
            if (avatarFile != null && !avatarFile.isEmpty()) {
                avatarUrl = cloudinaryService.upload(avatarFile);
            }
            memberService.updateMember(memberId, memberRequest);
            if (avatarUrl != null) {
                memberService.updateAvatar(memberId, avatarUrl);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hội viên thành công!");
            return "redirect:/members";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("memberId", memberId);
            model.addAttribute("pageTitle", "Chỉnh sửa Hội viên");
            model.addAttribute("contentView", "member-form");
            model.addAttribute("activePage", "members"); // <-- BÁO ACTIVE
            return "fragments/layout";
        }
    }

    @GetMapping("/detail/{memberId}")
    public String getMemberDetailPage(@PathVariable("memberId") Long memberId, Model model, RedirectAttributes redirectAttributes) {
        try {
            MemberResponseDTO member = memberService.getMemberById(memberId);
            model.addAttribute("memberProfile", member);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể mở chi tiết hội viên: " + e.getMessage());
            return "redirect:/members";
        }

        try {
            List<SubscriptionResponseDTO> subscriptions = subscriptionService.getSubscriptionsByMemberId(memberId);
            model.addAttribute("memberSubscriptions", subscriptions);
            
            // Lọc gói để gia hạn: chỉ hiển thị gói cùng loại với gói đang ACTIVE của member
            // Chỉ lấy các subscription có status ACTIVE
            java.util.Set<com.gym.service.gymmanagementservice.models.PackageType> activePackageTypes = subscriptions.stream()
                    .filter(sub -> sub.getStatus() != null && 
                            sub.getStatus() == com.gym.service.gymmanagementservice.models.SubscriptionStatus.ACTIVE && 
                            sub.getPackageType() != null)
                    .map(SubscriptionResponseDTO::getPackageType)
                    .collect(java.util.stream.Collectors.toSet());
            
            // Danh sách gói cho phần gia hạn: chỉ gói cùng loại với gói đang ACTIVE và gói phải đang active
            // Chỉ hiển thị khi member có ít nhất 1 subscription ACTIVE
            List<PackageResponseDTO> renewPackages = new java.util.ArrayList<>();
            if (!activePackageTypes.isEmpty()) {
                renewPackages = packageService.getAllPackages().stream()
                        .filter(PackageResponseDTO::isActive) // Chỉ gói đang active
                        .filter(pkg -> activePackageTypes.contains(pkg.getPackageType())) // Chỉ cùng loại với gói đang ACTIVE
                        .collect(Collectors.toList());
            }
            model.addAttribute("renewPackages", renewPackages);
        } catch (Exception e) {
            model.addAttribute("memberSubscriptions", java.util.Collections.emptyList());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("renewPackages", java.util.Collections.emptyList());
        }

        // Danh sách tất cả gói active cho việc đăng ký mới
        try {
            List<PackageResponseDTO> allPackages = packageService.getAllPackages().stream()
                    .filter(PackageResponseDTO::isActive)
                    .collect(Collectors.toList());
            model.addAttribute("allPackages", allPackages);
        } catch (Exception e) {
            model.addAttribute("allPackages", java.util.Collections.emptyList());
        }

        try {
            List<UserResponseDTO> allPts = staffService.getAllPts();
            model.addAttribute("allPts", allPts);
        } catch (Exception e) {
            model.addAttribute("allPts", java.util.Collections.emptyList());
        }

        try {
            List<MemberResponseDTO> allMembers = memberService.getAllMembers();
            model.addAttribute("allMembers", allMembers);
        } catch (Exception e) {
            model.addAttribute("allMembers", java.util.Collections.emptyList());
        }

        if (!model.containsAttribute("subRequest")) {
            SubscriptionRequestDTO subRequest = new SubscriptionRequestDTO();
            subRequest.setMemberId(memberId);
            model.addAttribute("subRequest", subRequest);
        }
        if (!model.containsAttribute("freezeRequest")) {
            model.addAttribute("freezeRequest", new FreezeRequestDTO());
        }
        if (!model.containsAttribute("logPtRequest")) {
            model.addAttribute("logPtRequest", new com.gym.service.gymmanagementservice.dtos.PtLogRequestDTO());
        }
        // Get check-in logs for this member
        try {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 100);
            java.util.List<com.gym.service.gymmanagementservice.models.CheckInLog> checkInLogs = 
                    checkInService.getCheckInLogs(memberId, null, null, null, pageable);
            java.util.List<com.gym.service.gymmanagementservice.dtos.CheckInLogResponseDTO> checkInLogDTOs = 
                    checkInLogs.stream()
                            .map(com.gym.service.gymmanagementservice.dtos.CheckInLogResponseDTO::fromCheckInLog)
                            .collect(java.util.stream.Collectors.toList());
            model.addAttribute("checkInLogs", checkInLogDTOs);
        } catch (Exception e) {
            model.addAttribute("checkInLogs", java.util.Collections.emptyList());
        }
        
        model.addAttribute("pageTitle", "Hội viên");
        model.addAttribute("contentView", "member-detail");
        model.addAttribute("activePage", "members");
        return "fragments/layout";
    }

    @GetMapping("/detail/{memberId}/transactions")
    public String getMemberTransactionsPage(@PathVariable("memberId") Long memberId, Model model, RedirectAttributes redirectAttributes) {
        try {
            MemberResponseDTO member = memberService.getMemberById(memberId);
            model.addAttribute("memberProfile", member);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể mở chi tiết hội viên: " + e.getMessage());
            return "redirect:/members";
        }

        try {
            java.util.List<com.gym.service.gymmanagementservice.dtos.TransactionReportDTO> timeline = reportService.getMemberTimeline(memberId);
            model.addAttribute("memberTimeline", timeline);
        } catch (Exception e) {
            model.addAttribute("memberTimeline", java.util.Collections.emptyList());
            model.addAttribute("errorMessage", e.getMessage());
        }

        model.addAttribute("pageTitle", "Lịch sử thanh toán");
        model.addAttribute("contentView", "member-transactions");
        model.addAttribute("activePage", "members");
        return "fragments/layout";
    }

    @GetMapping("/{memberId}/subscribe")
    public String showSubscribeForm(@PathVariable("memberId") Long memberId, Model model, RedirectAttributes redirectAttributes) {
        try {
            MemberResponseDTO member = memberService.getMemberById(memberId);
            model.addAttribute("memberProfile", member);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể mở đăng ký gói: " + e.getMessage());
            return "redirect:/members";
        }
        try {
            List<PackageResponseDTO> allPackages = packageService.getAllPackages().stream()
                    .filter(PackageResponseDTO::isActive)
                    .collect(Collectors.toList());
            model.addAttribute("allPackages", allPackages);
        } catch (Exception e) {
            model.addAttribute("allPackages", java.util.Collections.emptyList());
        }
        try {
            List<UserResponseDTO> allPts = staffService.getAllPts();
            model.addAttribute("allPts", allPts);
        } catch (Exception e) {
            model.addAttribute("allPts", java.util.Collections.emptyList());
        }
        SubscriptionRequestDTO subRequest = new SubscriptionRequestDTO();
        subRequest.setMemberId(memberId);
        model.addAttribute("subRequest", subRequest);
        model.addAttribute("pageTitle", "Đăng ký gói cho hội viên");
        model.addAttribute("contentView", "member-subscribe");
        model.addAttribute("activePage", "members");
        return "fragments/layout";
    }

    @PostMapping("/{memberId}/subscribe")
    public String processSubscribe(jakarta.servlet.http.HttpServletRequest httpRequest,
                                   @PathVariable("memberId") Long memberId,
                                   @Valid @ModelAttribute("subRequest") SubscriptionRequestDTO subRequest,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("memberProfile", memberService.getMemberById(memberId));
            model.addAttribute("allPackages", packageService.getAllPackages());
            model.addAttribute("allPts", staffService.getAllPts());
            model.addAttribute("pageTitle", "Đăng ký gói cho hội viên");
            model.addAttribute("contentView", "member-subscribe");
            model.addAttribute("activePage", "members");
            return "fragments/layout";
        }
        try {
            // Kiểm tra payment method
            if (subRequest.getPaymentMethod() == com.gym.service.gymmanagementservice.models.PaymentMethod.VN_PAY) {
                // Thanh toán qua VNPay
                String vnpUrl = paymentService.createSubscriptionPaymentUrl(httpRequest, memberId, subRequest.getPackageId());
                return "redirect:" + vnpUrl;
            } else {
                // Thanh toán tiền mặt hoặc chuyển khoản - xử lý trực tiếp
                subscriptionService.createSubscription(subRequest);
                redirectAttributes.addFlashAttribute("successMessage", "Đã đăng ký gói thành công.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/members/detail/" + memberId;
    }

    @GetMapping("/{memberId}/subscribe/preview")
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> previewSubscribe(@PathVariable("memberId") Long memberId,
                                                                                                   @RequestParam("packageId") Long packageId) {
        try {
            java.util.Map<String, Object> map = subscriptionService.estimateSubscriptionBreakdown(memberId, packageId);
            return org.springframework.http.ResponseEntity.ok(map);
        } catch (Exception e) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("error", e.getMessage());
            return org.springframework.http.ResponseEntity.status(400).body(map);
        }
    }

    @GetMapping("/packages/{packageId}/available-time-slots")
    public org.springframework.http.ResponseEntity<?> getAvailableTimeSlots(
            @PathVariable("packageId") Long packageId) {
        try {
            java.util.List<java.util.Map<String, Object>> slots = subscriptionService.getAvailableTimeSlots(packageId);
            return org.springframework.http.ResponseEntity.ok(slots);
        } catch (Exception e) {
            java.util.Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            return org.springframework.http.ResponseEntity.status(400).body(error);
        }
    }

    @PostMapping("/subscription/{action}")
    public String processSubscriptionAction(jakarta.servlet.http.HttpServletRequest httpRequest,
                                           @PathVariable("action") String action, 
                                           @Valid @ModelAttribute("subRequest") SubscriptionRequestDTO request, 
                                           BindingResult bindingResult, 
                                           RedirectAttributes redirectAttributes) {
        String redirectUrl = "redirect:/members/detail/" + request.getMemberId();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu không hợp lệ.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.subRequest", bindingResult);
            redirectAttributes.addFlashAttribute("subRequest", request);
            return redirectUrl;
        }
        try {
            if ("create".equals(action)) {
                // Kiểm tra payment method
                if (request.getPaymentMethod() == com.gym.service.gymmanagementservice.models.PaymentMethod.VN_PAY) {
                    // Thanh toán qua VNPay
                    String vnpUrl = paymentService.createSubscriptionPaymentUrl(httpRequest, request.getMemberId(), request.getPackageId());
                    return "redirect:" + vnpUrl;
                } else {
                    subscriptionService.createSubscription(request);
                    redirectAttributes.addFlashAttribute("successMessage", "Thêm gói tập thành công!");
                }
            } else if ("renew".equals(action)) {
                // Gia hạn cũng cần check payment method
                if (request.getPaymentMethod() == com.gym.service.gymmanagementservice.models.PaymentMethod.VN_PAY) {
                    // Thanh toán qua VNPay
                    String vnpUrl = paymentService.createRenewPaymentUrl(httpRequest, request.getMemberId(), 
                            request.getPackageId(), request.getAssignedPtId());
                    return "redirect:" + vnpUrl;
                } else {
                    subscriptionService.renewSubscription(request);
                    redirectAttributes.addFlashAttribute("successMessage", "Gia hạn gói tập thành công!");
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return redirectUrl;
    }

    @PostMapping("/subscription/freeze/{subId}")
    public String processFreeze(@PathVariable("subId") Long subId, @RequestParam("memberId") Long memberId, @Valid @ModelAttribute("freezeRequest") FreezeRequestDTO request, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        String redirectUrl = "redirect:/members/detail/" + memberId;
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số ngày đóng băng phải lớn hơn 0.");
            return redirectUrl;
        }
        try {
            subscriptionService.freezeSubscription(subId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Đóng băng gói tập thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return redirectUrl;
    }

    @PostMapping("/subscription/unfreeze/{subId}")
    public String processUnfreeze(@PathVariable("subId") Long subId, @RequestParam("memberId") Long memberId, RedirectAttributes redirectAttributes) {
        try {
            subscriptionService.unfreezeSubscription(subId);
            redirectAttributes.addFlashAttribute("successMessage", "Mở băng gói tập thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/members/detail/" + memberId;
    }

    @PostMapping("/subscription/cancel/{subId}")
    public String processCancel(@PathVariable("subId") Long subId, @RequestParam("memberId") Long memberId, RedirectAttributes redirectAttributes) {
        try {
            subscriptionService.cancelSubscription(subId);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy gói tập thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/members/detail/" + memberId;
    }

    @PostMapping("/subscription/cancel-refund/{subId}")
    public String processCancelAndRefund(@PathVariable("subId") Long subId,
                                         @RequestParam("memberId") Long memberId,
                                         @RequestParam("paymentMethod") com.gym.service.gymmanagementservice.models.PaymentMethod paymentMethod,
                                         @RequestParam(value = "reason", required = false) String reason,
                                         RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== CANCEL-REFUND REQUEST ===");
            System.out.println("Subscription ID: " + subId);
            System.out.println("Member ID: " + memberId);
            System.out.println("Payment Method: " + paymentMethod);
            System.out.println("Reason: " + reason);
            
            subscriptionService.cancelSubscription(subId, reason);
            java.math.BigDecimal refunded = subscriptionService.refundSubscription(subId, paymentMethod);
            
            System.out.println("Refunded amount: " + refunded);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy gói và hoàn tiền: " + refunded + " VNĐ.");
        } catch (Exception e) {
            System.err.println("ERROR in cancel-refund: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/members/detail/" + memberId;
    }

    @PostMapping("/subscription/checkout/{subId}")
    public String processCheckout(@PathVariable("subId") Long subId,
                                  @RequestParam("memberId") Long memberId,
                                  RedirectAttributes redirectAttributes) {
        try {
            com.gym.service.gymmanagementservice.dtos.CheckInResponseDTO res = checkInService.performCheckoutBySubscription(subId);
            if (res.getSessionDurationSeconds() != null) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã ghi giờ ra. Thời lượng: " + res.getSessionDurationSeconds() + "s");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã ghi giờ ra.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/members/detail/" + memberId;
    }

    @PostMapping("/subscription/upgrade/{subId}")
    public String processUpgrade(jakarta.servlet.http.HttpServletRequest httpRequest,
                                 @PathVariable("subId") Long subId,
                                 @RequestParam("memberId") Long memberId,
                                 @RequestParam("newPackageId") Long newPackageId,
                                 @RequestParam("paymentMethod") com.gym.service.gymmanagementservice.models.PaymentMethod paymentMethod,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra payment method
            if (paymentMethod == com.gym.service.gymmanagementservice.models.PaymentMethod.VN_PAY) {
                // Thanh toán qua VNPay
                String vnpUrl = paymentService.createUpgradePaymentUrl(httpRequest, subId, newPackageId);
                return "redirect:" + vnpUrl;
            } else {
                subscriptionService.upgradeSubscription(subId, newPackageId, paymentMethod);
                redirectAttributes.addFlashAttribute("successMessage", "Nâng cấp gói tập thành công.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/members/detail/" + memberId;
    }

    @PostMapping("/subscription/transfer/{subId}")
    public String processTransfer(@PathVariable("subId") Long subId,
                                  @RequestParam("memberId") Long memberId,
                                  @RequestParam("toMemberId") Long toMemberId,
                                  RedirectAttributes redirectAttributes) {
        try {
            subscriptionService.transferSubscription(subId, toMemberId);
            redirectAttributes.addFlashAttribute("successMessage", "Chuyển nhượng gói tập thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/members/detail/" + memberId;
    }

    @PostMapping("/subscription/log-pt/{subId}")
    public String processLogPtSession(@PathVariable("subId") Long subId, @RequestParam("memberId") Long memberId, @ModelAttribute("logPtRequest") com.gym.service.gymmanagementservice.dtos.PtLogRequestDTO request, RedirectAttributes redirectAttributes) {
        try {
            ptSessionService.logPtSession(subId, request.getNotes());
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận 1 buổi tập PT.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/members/detail/" + memberId;
    }

    @PostMapping("/delete/{memberId}")
    public String deleteMember(@PathVariable("memberId") Long memberId, RedirectAttributes redirectAttributes) {
        try {
            memberService.deleteMember(memberId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa hội viên.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/members";
    }
}
