package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
public class MemberResponseDTO {

    @Schema(description = "ID của hồ sơ Hội viên (Member ID)")
    private Long id;

    @Schema(description = "ID của tài khoản (User ID) liên kết")
    private Long userAccountId;

    private String fullName;
    private String phoneNumber;
    private String email;
    private LocalDate birthDate;
    private String address;
    private String barcode;
    private OffsetDateTime createdAt;

    public static MemberResponseDTO fromMember(Member member) {
        return MemberResponseDTO.builder()
                .id(member.getId())
                .userAccountId(member.getUserAccount() != null ? member.getUserAccount().getId() : null)
                .fullName(member.getFullName())
                .phoneNumber(member.getPhoneNumber())
                .email(member.getEmail())
                .birthDate(member.getBirthDate())
                .address(member.getAddress())
                .barcode(member.getBarcode())
                .createdAt(member.getCreatedAt())
                .build();
    }
}