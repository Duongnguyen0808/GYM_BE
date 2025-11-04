package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.dtos.*;
import com.gym.service.gymmanagementservice.models.PackageType;
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

    @GetMapping
    public String getMembersPage(Model model) {
        List<MemberResponseDTO> members = memberService.getAllMembers();
        model.addAttribute("members", members);
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
    public String processCreateMember(@Valid @ModelAttribute("memberRequest") MemberRequestDTO memberRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
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
    public String processEditMember(@PathVariable("memberId") Long memberId, @Valid @ModelAttribute("memberRequest") MemberRequestDTO memberRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("memberId", memberId);
            model.addAttribute("pageTitle", "Chỉnh sửa Hội viên");
            model.addAttribute("contentView", "member-form");
            model.addAttribute("activePage", "members"); // <-- BÁO ACTIVE
            return "fragments/layout";
        }
        try {
            memberService.updateMember(memberId, memberRequest);
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
    public String getMemberDetailPage(@PathVariable("memberId") Long memberId, Model model) {
        try {
            MemberResponseDTO member = memberService.getMemberById(memberId);
            model.addAttribute("memberProfile", member);
            List<SubscriptionResponseDTO> subscriptions = subscriptionService.getSubscriptionsByMemberId(memberId);
            model.addAttribute("memberSubscriptions", subscriptions);
            List<PackageResponseDTO> allPackages = packageService.getAllPackages().stream()
                    .filter(PackageResponseDTO::isActive)
                    .collect(Collectors.toList());
            model.addAttribute("allPackages", allPackages);
            List<UserResponseDTO> allPts = staffService.getAllPts();
            model.addAttribute("allPts", allPts);
            if (!model.containsAttribute("subRequest")) {
                model.addAttribute("subRequest", new SubscriptionRequestDTO());
            }
            if (!model.containsAttribute("freezeRequest")) {
                model.addAttribute("freezeRequest", new FreezeRequestDTO());
            }
            if (!model.containsAttribute("logPtRequest")) {
                model.addAttribute("logPtRequest", new com.gym.service.gymmanagementservice.dtos.PtLogRequestDTO());
            }
            model.addAttribute("pageTitle", "Hội viên: " + member.getFullName());
            model.addAttribute("contentView", "member-detail");
            model.addAttribute("activePage", "members"); // <-- BÁO ACTIVE
            return "fragments/layout";
        } catch (Exception e) {
            return "redirect:/members";
        }
    }

    @PostMapping("/subscription/{action}")
    public String processSubscriptionAction(@PathVariable("action") String action, @Valid @ModelAttribute("subRequest") SubscriptionRequestDTO request, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        String redirectUrl = "redirect:/members/detail/" + request.getMemberId();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu không hợp lệ.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.subRequest", bindingResult);
            redirectAttributes.addFlashAttribute("subRequest", request);
            return redirectUrl;
        }
        try {
            if ("create".equals(action)) {
                subscriptionService.createSubscription(request);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm gói tập thành công!");
            } else if ("renew".equals(action)) {
                subscriptionService.renewSubscription(request);
                redirectAttributes.addFlashAttribute("successMessage", "Gia hạn gói tập thành công!");
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
}