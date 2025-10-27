package com.chat.trans;

import com.chat.config.AppProperties;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverPapagoTransClient {

    @Qualifier("transWebClient")
    private final WebClient transWebClient;

    private final AppProperties props;

    public Mono<String> translate(@Nullable String source, String target, String text){

        if (target == null || target.isBlank()) {
            return Mono.error(new IllegalArgumentException("target is required"));
        }
        if (text == null || text.isBlank()) {
            return Mono.just(text == null ? "" : text); // 번역할 값이 없으면 바로 반환
        }
        if (java.util.Objects.equals(source, target)) {
            return Mono.just(text);
        }
        var trans = props.getTrans();

//        var body = BodyInserters.fromFormData("source", source)
//                .with("target", target)
//                .with("text", text);
        MultiValueMap<String,String> body = new LinkedMultiValueMap<>();
        body.add("source",source);
        body.add("target",target);
        body.add("text",text);
        System.out.println(body);
        return transWebClient.post()
                .uri(uriBuilder-> uriBuilder
                        .path(trans.getPath())
                        .build()
                )
                .headers(header->{
                    header.set("X-NCP-APIGW-API-KEY-ID",trans.getApiKeyId());
                    header.set("X-NCP-APIGW-API-KEY",trans.getApiKey());
                })
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("(no body)")
                                .flatMap(b -> Mono.error(new IllegalStateException(
                                        "Papago HTTP " + resp.statusCode() + " body=" + b)))
                )
                .bodyToMono(String.class)
                .map(NaverPapagoTransClient::extractTranslatedText)
                .timeout(Duration.ofMillis(trans.getReadTimeoutMs()))
                .doOnNext(res-> log.info("[TRANS] response: {}",res))
                .doOnError(TimeoutException.class, e -> log.error("[Papago] timeout", e))
                .doOnError(WebClientResponseException.class, e -> log.error("[Papago] http={} body={}", e.getRawStatusCode(), e.getResponseBodyAsString(), e))
                .onErrorResume(e->{
                    log.error("[Papago] translate failed: ",e.toString());
                    return Mono.error(e);
                });
    }

    static String extractTranslatedText(String json){
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            var node = root.path("message").path("result").path("translatedText");
            if (node.isMissingNode() || node.isNull() || !node.isTextual()) {
                throw new IllegalArgumentException("translatedText missing");
            }
            return node.asText();
        }catch(Exception e){
            throw new IllegalArgumentException("Invalid Papago JSON: "+e.getMessage(),e);
        }
    }

}
