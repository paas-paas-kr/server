package com.member.service;

import com.common.dto.TokenResponse;
import com.common.enumtype.Language;

import com.member.domain.Member;
import com.member.dto.LoginRequest;
import com.member.dto.MemberResponse;
import com.member.dto.SignupRequest;
import com.common.exception.user.UserErrorCode;
import com.common.exception.user.UserException;
import com.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     */
    @Transactional
    public MemberResponse signup(SignupRequest request) {
        // 이메일 중복 검사
        if (memberRepository.existsByEmail(request.email())) {
            throw UserException.from(UserErrorCode.DUPLICATE_EMAIL);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 회원 생성
        Member member = Member.of(
            request.email(),
            encodedPassword,
            request.name(),
            Member.MemberRole.USER,
            Member.MemberStatus.ACTIVE,
            request.preferredLanguage()
        );
        Member savedMember = memberRepository.save(member);

        return MemberResponse.from(savedMember);
    }

    /**
     * 로그인
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        // 이메일로 회원 조회
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> UserException.from(UserErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw UserException.from(UserErrorCode.INVALID_PASSWORD);
        }

        // 계정 상태 확인
        if (member.getStatus() != Member.MemberStatus.ACTIVE) {
            throw UserException.from(UserErrorCode.INVALID_ACCOUNT_STATUS);
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(
            member.getId(),
            member.getEmail(),
            member.getRole().name(),
            member.getPreferredLanguage().name()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

        return TokenResponse.of(accessToken, refreshToken, jwtTokenProvider.getExpirationTime() / 1000);
    }

    /**
     * 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public MemberResponse getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> UserException.from(UserErrorCode.USER_NOT_FOUND));

        return MemberResponse.from(member);
    }

    /**
     * 언어 설정 변경
     */
    @Transactional
    public MemberResponse updatePreferredLanguage(Long memberId, Language preferredLanguage) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> UserException.from(UserErrorCode.USER_NOT_FOUND));

        member.updatePreferredLanguage(preferredLanguage);

        return MemberResponse.from(member);
    }

    /**
     * 언어 설정 변경 및 새로운 JWT 토큰 발급
     */
    @Transactional
    public TokenResponse updatePreferredLanguageAndReissueToken(Long memberId, Language preferredLanguage) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> UserException.from(UserErrorCode.USER_NOT_FOUND));

        member.updatePreferredLanguage(preferredLanguage);

        // 새로운 JWT 토큰 생성 (변경된 언어 정보 포함)
        String accessToken = jwtTokenProvider.generateAccessToken(
            member.getId(),
            member.getEmail(),
            member.getRole().name(),
            member.getPreferredLanguage().name()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

        return TokenResponse.of(accessToken, refreshToken, jwtTokenProvider.getExpirationTime() / 1000);
    }
}
