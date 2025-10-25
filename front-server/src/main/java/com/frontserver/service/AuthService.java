package com.frontserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final WebClient webClient;

    /**
     * 회원가입
     */
    public Mono<Map<String, Object>> signup(String email, String password, String name, String preferredLanguage) {
        Map<String, String> requestBody = Map.of(
                "email", email,
                "password", password,
                "name", name,
                "preferredLanguage", preferredLanguage
        );

        return webClient.post()
                .uri("/api/auth/signup")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * 로그인
     */
    public Mono<Map<String, Object>> login(String email, String password) {
        Map<String, String> requestBody = Map.of(
                "email", email,
                "password", password
        );

        return webClient.post()
                .uri("/api/auth/login")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * 내 정보 조회
     */
    public Mono<Map<String, Object>> getMyInfo(String token) {
        return webClient.get()
                .uri("/api/auth/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * 언어 설정 변경
     */
    public Mono<Map<String, Object>> updateLanguage(String token, String preferredLanguage) {
        Map<String, String> requestBody = Map.of("preferredLanguage", preferredLanguage);

        return webClient.patch()
                .uri("/api/auth/language")
                .header("Authorization", "Bearer " + token)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
