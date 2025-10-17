package com.chat.stt;

import com.chat.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverSttClient implements SttClient{

    @Qualifier("sttWebClient")
    private final WebClient sttWebClient;
    private final AppProperties props;

    /** WAV(PCM s16le/16k/mono) 바이트를 짧은 음성 인식 API에 전송 */
    public Mono<String> transcribe(byte[] wavBytes) {
        var stt = props.getStt();
        System.out.println(stt);

        return sttWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(stt.getPath())                  // /recog/v1
                        .queryParam("lang", stt.getLanguage())// Kor
                        .build())
                .headers(header->{
                    header.set("X-NCP-APIGW-API-KEY", stt.getApiKey());
                    header.set("X-NCP-APIGW-API-KEY-ID", stt.getApiKeyId());
                })
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(wavBytes)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(res -> log.info("[STT] response: {}", res));
    }
}