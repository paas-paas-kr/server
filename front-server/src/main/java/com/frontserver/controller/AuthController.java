package com.frontserver.controller;

import com.frontserver.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String login(Model model) {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        return "auth/signup";
    }

    /**
     * 회원가입 API
     */
    @PostMapping("/api/signup")
    @ResponseBody
    public Mono<ResponseEntity<Map<String, Object>>> signupApi(
            @RequestBody Map<String, String> request
    ) {
        String email = request.get("email");
        String password = request.get("password");
        String name = request.get("name");
        String preferredLanguage = request.get("preferredLanguage");

        return authService.signup(email, password, name, preferredLanguage)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("이미 존재")) {
                        return Mono.just(ResponseEntity.status(400).body(Map.of("error", "이미 가입된 이메일입니다.")));
                    }
                    return Mono.just(ResponseEntity.status(500).body(Map.of("error", "회원가입에 실패했습니다.")));
                });
    }

    /**
     * 로그인 API
     */
    @PostMapping("/api/login")
    @ResponseBody
    public Mono<ResponseEntity<Map<String, Object>>> loginApi(
            @RequestBody Map<String, String> request
    ) {
        String email = request.get("email");
        String password = request.get("password");

        return authService.login(email, password)
                .map(response -> {
                    // JWT 토큰을 쿠키에 저장
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    String accessToken = (String) data.get("accessToken");

                    ResponseCookie cookie = ResponseCookie.from("accessToken", accessToken)
                            .httpOnly(true)
                            .secure(false) // 개발환경에서는 false, 프로덕션에서는 true
                            .path("/")
                            .maxAge(24 * 60 * 60) // 1일
                            .build();

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, cookie.toString())
                            .body(response);
                })
                .onErrorResume(e -> {
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && (errorMessage.contains("401") || errorMessage.contains("인증"))) {
                        return Mono.just(ResponseEntity.status(401).body(Map.of("error", "이메일 또는 비밀번호가 올바르지 않습니다.")));
                    }
                    return Mono.just(ResponseEntity.status(500).body(Map.of("error", "로그인에 실패했습니다.")));
                });
    }

    /**
     * 로그아웃 API
     */
    @PostMapping("/api/logout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logoutApi() {
        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "로그아웃되었습니다."));
    }

    /**
     * 내 정보 조회 API
     */
    @GetMapping("/api/me")
    @ResponseBody
    public Mono<ResponseEntity<Map<String, Object>>> getMyInfo(
            @CookieValue(value = "accessToken", required = false) String token
    ) {
        if (token == null || token.isEmpty()) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }

        return authService.getMyInfo(token)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
    }

    /**
     * 언어 설정 변경 API
     */
    @PatchMapping("/api/language")
    @ResponseBody
    public Mono<ResponseEntity<Map<String, Object>>> updateLanguage(
            @CookieValue(value = "accessToken", required = false) String token,
            @RequestBody Map<String, String> request
    ) {
        if (token == null || token.isEmpty()) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }

        String preferredLanguage = request.get("preferredLanguage");

        return authService.updateLanguage(token, preferredLanguage)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
    }
}
