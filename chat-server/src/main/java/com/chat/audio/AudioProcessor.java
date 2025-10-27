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
import com.chat.conversation.service.ConversationService;
import com.chat.pipeline.LlmFirstRagOrchestrator;
import com.chat.stt.NaverSttClient;
import com.chat.trans.NaverPapagoTransClient;
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
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class AudioProcessor {

    private final SessionRegistry registry;
    private final AudioTranscoder transcoder;
    private final NaverSttClient sttClient;
    private final NaverPapagoTransClient transClient;
    private final AppProperties props;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ConversationService conversationService;

    private final LlmFirstRagOrchestrator rag;

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
        // 선택적으로 세션에 저장해둔 메타/roomId 를 꺼냄
        AudioMeta meta = emitter.getAttribute("audioMeta", AudioMeta.class);
        String roomId = emitter.getAttribute("roomId", String.class); // 세팅해뒀다면 사용
        if (roomId == null || roomId.isBlank()) {
            // 필요하다면 고정값/파라미터/메타에서 가져오도록 조정
         //   roomId = "gimin_room";
        }

        final String traceId = sessionId;

        return Mono.fromCallable(() -> {
                    // 1) 필요시 트랜스코딩 (webm/opus/ogg → wav 16k mono)
                    if (needsTranscode(mimeType)) {
                        return transcoder.webmOpusToPcmWav16kMono(mergedBytes);
                    }
                    return mergedBytes;
                })
                .subscribeOn(Schedulers.boundedElastic())

                // 2) STT
                .flatMap(wav -> {
                    if (meta == null || meta.getLang() == null || meta.getLang().isBlank()) {
                        return Mono.error(new IllegalStateException("lang is required (START)"));
                    }
                    Lang lang = Lang.fromClientCode(meta.getLang()).orElse(Lang.KOR);
                    return sttClient.transcribe(wav, lang.csr);
                })
                .map(this::extractTextField) // {"text": "..."}에서 text 추출
                .flatMap(text -> {
                    if (text == null || text.isBlank()) {
                        return Mono.error(new IllegalStateException("No text"));
                    }

                    emitter.emitText(chat("CHAT", text));
                    // 3) 번역 언어 설정 (원본→ko)
                    final String sourceLang = Lang.mapCsrToPapago(meta.getLang());
                    final String targetLang = "ko";

                    // 4) Papago (원본→한국어), 두 군데에서 쓰니 cache()
                    Mono<String> koMono = transClient.translate(sourceLang, targetLang, text).cache();

                    // 4-1) 번역본을 바로 사용자에게 송신 (ChatWebSocketHandler의 translatedFlow 역할)
                    Mono<Void> translatedFlow =
                            koMono
                                    .doOnNext(koText -> {
                                        log.info("[PROC:{}] TRANS KO: {}", sessionId, koText);
                                       // emitter.emitText(chat("TRANS", koText));
                                    })
                                    .then();

                    // 4-2) RAG→LLM(ko) → Papago 역번역(ko→원어) → 최종 송신 + Firestore 저장
                    Mono<Void> ragAndTranslateFlow =
                            koMono
                                    // RAG/LLM 실행: ko 입력, ko 답변 방출(Mono<String>)
                                    .flatMap(koUserText -> rag.run(koUserText, emitter))
                                    .doOnNext(koAnswer -> log.info("[PROC:{}] LLM(KO): {}", sessionId, koAnswer))
                                    // 역번역: ko → sourceLang
                                    .flatMap(koAnswer -> transClient.translate(targetLang, sourceLang, koAnswer))
                                    .doOnNext(finalAnswer -> {
                                        // 최종 답변을 클라이언트에 전송 (ChatWebSocketHandler와 동일 포맷)
                                        emitter.emitText(JsonUtils.toJson(Map.of(
                                                "type", "nlp-stream",
                                                "event", "original_text",
                                                "data", Map.of("text", finalAnswer),
                                                "traceId", traceId
                                        )));
                                    })
                                    // Fire-and-Forget 저장 (질문/답변 동시 저장)
                                    .doOnNext(finalAnswer -> {
                                        conversationService.createMessage(/*question*/ text, /*answer*/ finalAnswer, roomId)
                                                .subscribe(
                                                        saved -> log.info("[PROC:{}] 메시지 저장 성공: {}", sessionId, saved.getId()),
                                                        err -> log.error("[PROC:{}] 메시지 저장 실패: {}", sessionId, err.getMessage())
                                                );
                                    })
                                    .then();

                    // 5) 두 플로우 순차 실행 (번역 송신 → RAG/LLM/역번역/저장)
                    return translatedFlow.then(ragAndTranslateFlow);
                })

                // 6) 에러 핸들링 & 알림
                .doOnError(e -> {
                    log.error("[PROC:{}] Audio pipeline failed", sessionId, e);
                    emitter.emitText(system("오디오 처리 중 오류가 발생했습니다: " + e.getMessage()));
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

