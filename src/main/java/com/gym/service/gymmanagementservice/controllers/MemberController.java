package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.MemberRequestDTO;
import com.gym.service.gymmanagementservice.dtos.MemberResponseDTO;
import com.gym.service.gymmanagementservice.services.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
@Tag(name = "Member Management API", description = "Các API để quản lý hội viên")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @Operation(summary = "Tạo một hội viên mới")
    public ResponseEntity<MemberResponseDTO> createMember(@Valid @RequestBody MemberRequestDTO request) {
        MemberResponseDTO newMember = memberService.createMember(request);
        return new ResponseEntity<>(newMember, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả hội viên")
    public ResponseEntity<List<MemberResponseDTO>> getAllMembers() {
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    // Endpoint để lấy chi tiết một hội viên
    @GetMapping("/{memberId}")
    @Operation(summary = "Lấy thông tin chi tiết một hội viên bằng ID")
    public ResponseEntity<MemberResponseDTO> getMemberById(@PathVariable Long memberId) {
        return ResponseEntity.ok(memberService.getMemberById(memberId));
    }

    // Endpoint để cập nhật thông tin hội viên
    @PutMapping("/{memberId}")
    @Operation(summary = "Cập nhật thông tin của một hội viên")
    public ResponseEntity<MemberResponseDTO> updateMember(@PathVariable Long memberId, @Valid @RequestBody MemberRequestDTO request) {
        MemberResponseDTO updatedMember = memberService.updateMember(memberId, request);
        return ResponseEntity.ok(updatedMember);
    }
}
