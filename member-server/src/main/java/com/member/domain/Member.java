package com.member.domain;

import com.common.enumtype.Language;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language preferredLanguage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum MemberRole {
        USER, ADMIN
    }

    public enum MemberStatus {
        ACTIVE, INACTIVE, DELETED
    }

    @Builder(access = AccessLevel.PRIVATE)
    private Member(String email, String password, String name, MemberRole role, MemberStatus status,
                   Language preferredLanguage
    ) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.status = status;
        this.preferredLanguage = preferredLanguage;
    }

    public static Member of(final String email, final String password, final String name, final MemberRole role,
                            final MemberStatus status, final Language preferredLanguage) {
        return Member.builder()
                .email(email)
                .password(password)
                .name(name)
                .role(role)
                .status(status)
                .preferredLanguage(preferredLanguage)
                .build();
    }

    public void updatePreferredLanguage(final Language preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
}