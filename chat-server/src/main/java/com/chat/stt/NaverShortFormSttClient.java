package com.chat.stt;

import com.chat.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverShortFormSttClient {

    private final WebClient sttWebClient;
    private final AppProperties props;

    /** WAV(PCM s16le/16k/mono) 바이트를 짧은 음성 인식 API에 전송 */
    public Mono<String> recognizeWav(byte[] wavBytes) {
        var stt = props.getStt();

        return sttWebClient.post()
                .uri(uri -> uri
                        .path(stt.getPath())                  // /recog/v1/stt
                        .queryParam("lang", stt.getLanguage())// Kor
                        .build())
                .header("X-NCP-APIGW-API-KEY-ID", stt.getApiKeyId())
                .header("X-NCP-APIGW-API-KEY", stt.getApiKey())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(wavBytes)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(res -> log.info("[STT] response: {}", res));
    }
}