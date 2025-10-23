package com.member.controller;

import com.common.dto.TokenResponse;
import com.common.response.DataResponse;
import com.common.security.GatewayUserDetails;
import com.member.dto.LoginRequest;
import com.member.dto.MemberResponse;
import com.member.dto.SignupRequest;
import com.member.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    public ResponseEntity<DataResponse<MemberResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        MemberResponse response = authService.signup(request);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    public ResponseEntity<DataResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다. (JWT 필요)")
    public ResponseEntity<DataResponse<MemberResponse>> getMyInfo(
            @AuthenticationPrincipal GatewayUserDetails userDetails
    ) {
        MemberResponse response = authService.getMemberInfo(userDetails.getUserId());
        return ResponseEntity.ok(DataResponse.from(response));
    }
}
