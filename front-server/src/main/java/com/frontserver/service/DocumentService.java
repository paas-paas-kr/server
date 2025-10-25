package com.frontserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final WebClient webClient;

    /**
     * 문서 목록 조회
     */
    public Mono<Map<String, Object>> getDocumentList(String token) {
        return webClient.get()
                .uri("/api/documents")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * 문서 업로드
     */
    public Mono<Map<String, Object>> uploadDocument(String token, MultipartFile file, String language) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", file.getResource());
        if (language != null && !language.isEmpty()) {
            builder.part("language", language);
        }

        return webClient.post()
                .uri("/api/documents/upload/files")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * 작업 상태 조회
     */
    public Mono<Map<String, Object>> getJobStatus(String token, Long jobId) {
        return webClient.get()
                .uri("/api/documents/" + jobId + "/status")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * 이미지 업로드
     */
    public Mono<Map<String, Object>> uploadImages(String token, List<MultipartFile> images, String language) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        for (MultipartFile image : images) {
            builder.part("images", image.getResource());
        }
        if (language != null && !language.isEmpty()) {
            builder.part("language", language);
        }

        return webClient.post()
                .uri("/api/documents/upload/images")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * 요약 결과 조회
     */
    public Mono<Map<String, Object>> getSummary(String token, Long jobId) {
        return webClient.get()
                .uri("/api/documents/" + jobId + "/summary")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
