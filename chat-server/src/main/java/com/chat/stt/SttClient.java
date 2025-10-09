package com.chat.stt;

import com.chat.stt.model.SttRequest;
import com.chat.stt.model.SttResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SttClient {
    private final WebClient sttWebClient;

    public Mono<SttResult> transcribeBatch(SttRequest req) {
        // TODO: 실제 STT API 스펙에 맞게 변경
        return sttWebClient.post()
                .uri("/v1/transcribe")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(SttResult.class);
    }
}