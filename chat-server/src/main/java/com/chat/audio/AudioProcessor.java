package com.chat.audio;



import com.chat.audio.model.AudioChunk;
import com.chat.audio.model.AudioMeta;
import com.chat.common.Lang;
import com.chat.common.json.JsonUtils;
import com.chat.common.ws.SessionRegistry;
import com.chat.common.ws.WsEmitter;
//import com.chat.llm.LlmClient;
//import com.chat.llm.model.LlmRequest;
//import com.chat.stt.SttClient;
//import com.chat.stt.model.SttRequest;
//import com.chat.stt.model.SttResult;
//import com.chat.tts.TtsClient;
//import com.chat.tts.model.TtsAudio;
//import com.chat.tts.model.TtsRequest;
import com.chat.config.AppProperties;
import com.chat.stt.NaverSttClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;


@Slf4j
@Component
@RequiredArgsConstructor
public class AudioProcessor {

    private final SessionRegistry registry;
    private final AudioTranscoder transcoder;
    private final NaverSttClient sttClient;
    private final AppProperties props;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void onMeta(String sessionId, AudioMeta meta) {
        log.info("[PROC:{}] meta: {}", sessionId, meta);
        WsEmitter e = registry.get(sessionId);
        if (e == null) return;
    }

    public void onChunk(String sessionId, AudioChunk chunk) {
        WsEmitter e = registry.get(sessionId);
        log.info("[PROC:{}] chunk ts={}ms bytes={}", sessionId, chunk.getTsMs(), chunk.getBytes().length);
        if (e == null) return;
    }

    public void complete(String sessionId) {
        log.info("[PROC:{}] complete", sessionId);
    }

    public Mono<Void> processFinal(String sessionId, byte[] mergedBytes, String mimeType, WsEmitter emitter) {

        return Mono.fromCallable(() -> {
                     if (needsTranscode(mimeType)) {
                        return transcoder.webmOpusToPcmWav16kMono(mergedBytes);
                    }
                    return mergedBytes;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(wav -> {
                    AudioMeta meta= emitter.getAttribute("audioMeta",AudioMeta.class);
                    if (meta == null || meta.getLang() == null || meta.getLang().isBlank()) {
                        return Mono.error(new IllegalStateException("lang is required (START)"));
                    }
                    Lang lang = Lang.fromClientCode(meta.getLang()).orElse(Lang.KOR);

                    return sttClient.transcribe(wav,lang.csr);
                })
                .map(this::extractTextField)
                // .flatMap(translate::toEnglish)  // 번역 (papago 질의)
                // .flatMap(llm:ask)               // LLM 질의
                .doOnNext(text -> {
                    log.info("[PROC:{}] STT: {}", sessionId, text);
                    emitter.emitText(chat("TRANS", text));
                })
                .doOnError(e -> {
                    log.error("[PROC:{}] STT failed", sessionId, e);
                    emitter.emitText(system("STT 변환 중 오류가 발생했습니다: " + e.getMessage()));
                })
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    private boolean needsTranscode(String mimeType) {
        if (mimeType == null) return true;
        String m = mimeType.toLowerCase();
        return (m.contains("webm") || m.contains("opus") || m.contains("ogg")) && !m.contains("wav");
    }

    private static String chat(String type, String txt) {
        return "{\"type\":\"" + type + "\",\"text\":\"" + escape(txt) + "\"}";
    }

    private static String system(String msg) {
        return "{\"type\":\"SYSTEM\",\"text\":\"" + escape(msg) + "\"}";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Nullable
    private String extractTextField(String json) {
        try {
            System.out.println(json);
            JsonNode root = MAPPER.readTree(json);
            JsonNode text = root.path("text");
            if(text.isMissingNode()||text.isNull()||!text.isTextual()){
                return null;
            }
            return text.asText();
         } catch (Exception e) {
            log.warn("Bad STT JSON",e);
            return null;
        }
    }

}

