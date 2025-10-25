package com.frontserver.controller;

import com.frontserver.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public String documentList(Model model) {
        return "document/list";
    }

    /**
     * 문서 목록 조회 API
     */
    @GetMapping("/api/list")
    @ResponseBody
    public Mono<ResponseEntity<Map<String, Object>>> getDocumentList(
            @CookieValue(value = "accessToken", required = false) String token
    ) {
        if (token == null || token.isEmpty()) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }

        return documentService.getDocumentList(token)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
    }

    /**
     * 문서 업로드 API
     */
    @PostMapping(value = "/api/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<Map<String, Object>>> uploadDocument(
            @CookieValue(value = "accessToken", required = false) String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", defaultValue = "ko") String language
    ) {
        if (token == null || token.isEmpty()) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }

        return documentService.uploadDocument(token, file, language)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
    }

    /**
     * 이미지 업로드 API
     */
    @PostMapping(value = "/api/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<Map<String, Object>>> uploadImages(
            @CookieValue(value = "accessToken", required = false) String token,
            @RequestParam("images") MultipartFile[] images,
            @RequestParam(value = "language", defaultValue = "ko") String language
    ) {
        if (token == null || token.isEmpty()) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }

        return documentService.uploadImages(token, java.util.Arrays.asList(images), language)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
    }

    /**
     * 작업 상태 조회 API
     */
    @GetMapping("/api/{jobId}/status")
    @ResponseBody
    public Mono<ResponseEntity<Map<String, Object>>> getJobStatus(
            @CookieValue(value = "accessToken", required = false) String token,
            @PathVariable Long jobId
    ) {
        if (token == null || token.isEmpty()) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }

        return documentService.getJobStatus(token, jobId)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
    }

    /**
     * 요약 결과 조회 API
     */
    @GetMapping("/api/{jobId}/summary")
    @ResponseBody
    public Mono<ResponseEntity<Map<String, Object>>> getSummary(
            @CookieValue(value = "accessToken", required = false) String token,
            @PathVariable Long jobId
    ) {
        if (token == null || token.isEmpty()) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }

        return documentService.getSummary(token, jobId)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
    }
}
