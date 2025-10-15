package com.document.application.summary.ocr.client;

import com.common.config.NaverOcrConfig;
import com.common.exception.document.DocumentErrorCode;
import com.common.exception.document.DocumentException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClovaOcrClient {

    private final NaverOcrConfig ocrConfig;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(ocrConfig.getApiUrl())
                .build();
    }

    /**
     * URL 기반으로 OCR 텍스트 추출
     */
    public String extractText(String fileUrl) {
        try {
            String requestBody = buildOcrMessage(fileUrl);
            JsonNode response = callOcrApi(requestBody);

            return parseOcrResponse(response);
        } catch (Exception e) {
            log.error("OCR 처리 중 예외 발생 - URL: {}, 오류: {}", fileUrl, e.getMessage(), e);
            throw DocumentException.from(DocumentErrorCode.DOCUMENT_PROCESSING_ERROR);
        }
    }

    private String buildOcrMessage(String fileUrl) {
        String format = getFileExtension(fileUrl);
        String fileName = extractFileName(fileUrl);

        return """
            {
              "version": "V2",
              "requestId": "%s",
              "timestamp": %d,
              "images": [
                {
                  "format": "%s",
                  "name": "%s",
                  "url": "%s"
                }
              ]
            }
            """.formatted(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                format,
                fileName,
                fileUrl
        );
    }

    private String extractFileName(String fileUrl) {
        String[] parts = fileUrl.split("/");
        return parts[parts.length - 1];
    }

    private String getFileExtension(String fileUrl) {
        String fileName = extractFileName(fileUrl);
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "jpg";
    }

    private JsonNode callOcrApi(String requestBody) {
        return webClient.post()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("X-OCR-SECRET", ocrConfig.getSecretKey())
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    private String parseOcrResponse(JsonNode response) {
        StringBuilder result = new StringBuilder();

        JsonNode fields = response.path("images").path(0).path("fields");
        if (fields.isArray()) {
            for (JsonNode field : fields) {
                String text = field.path("inferText").asText("");
                if (!text.isEmpty()) {
                    result.append(text).append(" ");
                }
            }
        }

        return result.toString().trim();
    }
}