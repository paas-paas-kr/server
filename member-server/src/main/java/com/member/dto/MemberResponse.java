package com.member.dto;

import com.member.domain.Member;

import java.time.LocalDateTime;

public record MemberResponse(
    Long id,
    String email,
    String name,
    String role,
    String status,
    String preferredLanguage,
    LocalDateTime createdAt

) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole().name(),
                member.getStatus().name(),
                member.getPreferredLanguage(),
                member.getCreatedAt()
        );
    }
}


