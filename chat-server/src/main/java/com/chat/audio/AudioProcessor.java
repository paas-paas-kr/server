package com.chat.audio;



import com.chat.audio.model.AudioChunk;
import com.chat.audio.model.AudioMeta;
import com.chat.chat.model.ChatOutbound;
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
import com.chat.stt.SttClient;
import com.chat.stt.model.SttRequest;
import com.chat.stt.model.SttResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;


@Slf4j
@Component
@RequiredArgsConstructor
public class AudioProcessor {

    private final SessionRegistry registry;

    // 모킹 설정
    @Value("${app.audio.mock.enabled:false}")
    boolean mockEnabled;

    @Value("${app.audio.mock.on:META}")
    String mockOn;

    @Value("${app.audio.mock.sample:classpath:/mock/sample-tts.ogg}")
    org.springframework.core.io.Resource mockSample;

    private byte[] mockBytes;

    @PostConstruct
    void loadMock() throws IOException {
        if (mockSample != null && mockSample.exists()) {
            try (var in = mockSample.getInputStream()) {
                this.mockBytes = in.readAllBytes();  // 미리 메모리에 캐시
            }
            log.info("[PROC] mock sample loaded: {} bytes", mockBytes.length);
        }
    }

    public void onMeta(String sessionId, AudioMeta meta) {
        log.info("[PROC:{}] meta: {}", sessionId, meta);
        var e = registry.get(sessionId);
        if (e == null) return;

        if (mockEnabled && "META".equalsIgnoreCase(mockOn) && mockBytes != null) {
            e.emitText(JsonUtils.toJson(new Msg("transcript", "(mock) 안녕하세요!")));
            e.emitBinary(mockBytes); // ★ STT 없이 즉시 오디오 내려보냄
        }
    }

    public void onChunk(String sessionId, AudioChunk chunk) {
        var e = registry.get(sessionId);
        if (e == null) return;

        if (mockEnabled && "CHUNK".equalsIgnoreCase(mockOn) && mockBytes != null) {
            e.emitText(JsonUtils.toJson(new Msg("transcript", "(mock) chunk received")));
            e.emitBinary(mockBytes); // ★ 매 청크마다 샘플 재생 (테스트용)
            return;                  // STT 경로로 가지 않음
        }

        // ↓ 실제 STT 경로 (모킹 꺼졌을 때만)
        // sttClient.transcribeBatch(...).flatMap(...).subscribe();
    }

    public void complete(String sessionId) { log.info("[PROC:{}] complete", sessionId); }

    record Msg(String type, String text) {}
}







//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class AudioProcessor {
//
//    private final SessionRegistry registry;
//    private final SttClient sttClient;
////    private final LlmClient llmClient;
////    private final TtsClient ttsClient;
//
//    /** 세션별 메타 저장이 필요하면 간단 Map<String, AudioMeta>를 추가해도 됩니다. */
//    // private final ConcurrentMap<String, AudioMeta> metas = new ConcurrentHashMap<>();
//
//    public void onMeta(String sessionId, AudioMeta meta) {
//        log.info("[PROC:{}] meta: {}", sessionId, meta);
//        // metas.put(sessionId, meta);
//        // 필요 시 초기 안내를 내려보내도 됨:
//        WsEmitter e = registry.get(sessionId);
//        if (e != null) {
//            e.emitText(JsonUtils.toJson(new Msg("status", "meta received: " + meta.getMimeType())));
//        }
//    }
//
//    public void onChunk(String sessionId, AudioChunk chunk) {
//        WsEmitter emitter = registry.get(sessionId);
//        if (emitter == null) {
//            log.debug("[PROC:{}] emitter not found, dropping chunk", sessionId);
//            return;
//        }
//
//
//        // 데모 단순화: 청크마다 배치 STT 호출
//        // 실제 서비스에선 세그먼트 큐/타이머로 모아서 호출하세요.
//        SttRequest sttReq = new SttRequest(
//                chunk.getBytes(),
//                /* meta에서 꺼내도 됨 */ "audio/webm;codecs=opus",
//                /* meta에서 꺼내도 됨 */ 48000
//        );
//
//        sttClient.transcribeBatch(sttReq)
//                .flatMap(stt -> handleSttResult(sessionId, stt, emitter))
//                .onErrorResume(e -> {
//                    log.warn("[PROC:{}] STT error: {}", sessionId, e.toString());
//                    emitter.emitText(JsonUtils.toJson(new Msg("error", "stt failed")));
//                    return Mono.empty();
//                })
//                .subscribe(); // fire-and-forget (세션 파이프라인과 독립 실행)
//    }
//
//    public void complete(String sessionId) {
//        log.info("[PROC:{}] complete", sessionId);
//        // metas.remove(sessionId);
//    }
//
//    private Mono<Void> handleSttResult(String sessionId, SttResult stt, WsEmitter emitter) {
//        if (stt == null || stt.getSegments() == null || stt.getSegments().isEmpty()) {
//            return Mono.empty();
//        }
//        // 간단화: 마지막 segment를 사용
//        var seg = stt.getSegments().get(stt.getSegments().size() - 1);
//        var text = seg.getText();
//
//        // 1) 자막(또는 중간결과) 먼저 내려줌
//        emitter.emitText(JsonUtils.toJson(new Msg("transcript", text)));
//
////        // 2) LLM → 3) TTS → 결과 푸시
////        return llmClient.complete(new LlmRequest(text))
////                .flatMap(llm -> {
////                    emitter.emit(JsonUtils.toJson(new Msg("answer", llm.getText())));
////                    return ttsClient.synthesize(new TtsRequest(llm.getText(), "basic", "audio/ogg"))
////                            .doOnNext(tts -> emitter.emitBinary(tts.getAudio()))
////                            .onErrorResume(e -> {
////                                log.warn("[PROC:{}] TTS error: {}", sessionId, e.toString());
////                                emitter.emit(JsonUtils.toJson(new Msg("error", "tts failed")));
////                                return Mono.empty();
////                            })
////                            .then();
////                })
////                .onErrorResume(e -> {
////                    log.warn("[PROC:{}] LLM error: {}", sessionId, e.toString());
////                    emitter.emit(JsonUtils.toJson(new Msg("error", "llm failed")));
////                    return Mono.empty();
////                });
//
//        return Mono.empty();
//    }
//
//    /** 간단 텍스트 메시지 DTO */
//    record Msg(String type, String text) {}
//}
