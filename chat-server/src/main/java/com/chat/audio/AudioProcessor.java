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
import com.chat.config.AppProperties;
import com.chat.stt.NaverShortFormSttClient;
import com.chat.stt.SttClient;
import com.chat.stt.model.SttRequest;
import com.chat.stt.model.SttResult;
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
    private final AudioTranscoder transcoder;            // webm/opus → WAV 변환용
    private final NaverShortFormSttClient sttClient;     // 네이버 STT REST 호출
    private final AppProperties props;                   // ffmpeg 경로 등 접근 가능

    // ----- Mock settings -----
    @Value("${app.audio.mock.enabled:false}")
    boolean mockEnabled;

    @Value("${app.audio.mock.on:META}")
    String mockOn; // META | CHUNK | FINAL

    @Value("${app.audio.mock.sample:classpath:/mock/sample-tts.ogg}")
    Resource mockSample;

    private byte[] mockBytes;

    @PostConstruct
    void loadMock() throws IOException {
        if (mockSample != null && mockSample.exists()) {
            try (var in = mockSample.getInputStream()) {
                this.mockBytes = in.readAllBytes();  // 미리 메모리에 캐시
            }
            log.info("[PROC] mock sample loaded: {} bytes", mockBytes.length);
        } else {
            log.info("[PROC] mock sample not found or disabled");
        }
    }

    // ----- Live event hooks -----

    /** 클라이언트가 META(TEXT)로 보낸 오디오 파라미터 수신 시 */
    public void onMeta(String sessionId, AudioMeta meta) {
        log.info("[PROC:{}] meta: {}", sessionId, meta);
        WsEmitter e = registry.get(sessionId);
        if (e == null) return;

        if (mockEnabled && "META".equalsIgnoreCase(mockOn) && mockBytes != null) {
            e.emitText(JsonUtils.toJson(new Msg("transcript", "(mock) 안녕하세요!")));
            e.emitBinary(mockBytes); // ★ STT 없이 즉시 오디오 내려보냄
        }
    }

    /** 오디오 청크 수신 시(선택: 실시간 처리/메트릭) */
    public void onChunk(String sessionId, AudioChunk chunk) {
        WsEmitter e = registry.get(sessionId);
        if (e == null) return;

        if (mockEnabled && "CHUNK".equalsIgnoreCase(mockOn) && mockBytes != null) {
            e.emitText(JsonUtils.toJson(new Msg("transcript", "(mock) chunk received")));
            e.emitBinary(mockBytes); // ★ 매 청크마다 샘플 재생 (테스트용)
            return;                  // STT 경로로 가지 않음
        }

        // 실제 실시간 STT가 아니라면(배치형), 여기서는 메트릭/로그만.
        log.debug("[PROC:{}] chunk ts={}ms bytes={}", sessionId, chunk.getTsMs(), chunk.getBytes().length);
    }

    public void complete(String sessionId) {
        log.info("[PROC:{}] complete", sessionId);
    }

    // ----- Finalization: 병합된 오디오를 STT에 태워 결과를 클라로 emit -----

    /**
     * FINISH 또는 소켓 종료 시 마지막으로 호출.
     * 1) (필요 시) webm/opus → WAV(PCM s16le, 16kHz, mono) 변환
     * 2) 네이버 Short-form STT REST 호출
     * 3) 결과 텍스트를 TRANS 타입으로 클라이언트에 emit
     */
    public Mono<Void> processFinal(String sessionId, byte[] mergedBytes, String mimeType, WsEmitter emitter) {

        // ★ Mock: FINAL 단계에서의 즉시 응답
        if (mockEnabled && "FINAL".equalsIgnoreCase(mockOn) && mockBytes != null) {
            emitter.emitText(JsonUtils.toJson(new Msg("transcript", "(mock) 최종 인식 결과입니다.")));
            emitter.emitBinary(mockBytes);
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
                    // 필요 시 변환
                    if (needsTranscode(mimeType)) {
                        return transcoder.webmOpusToPcmWav16kMono(mergedBytes);
                    }
                    return mergedBytes;
                })
                .subscribeOn(Schedulers.boundedElastic())   // ffmpeg 호출은 블로킹 → 분리
                .flatMap(wav -> sttClient.recognizeWav(wav)) // 네이버 STT 호출
                .map(this::extractTextField)                 // {"text":"..."} → "..."
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
        if (mimeType == null) return true; // 안전하게 변환
        String m = mimeType.toLowerCase();
        // webm/opus/ogg면 변환, wav/pcm이면 패스
        return (m.contains("webm") || m.contains("opus") || m.contains("ogg")) && !m.contains("wav");
    }

    // ----- small helpers -----

    private static String chat(String type, String txt) {
        return "{\"type\":\"" + type + "\",\"text\":\"" + escape(txt) + "\"}";
    }

    private static String system(String msg) {
        return "{\"type\":\"SYSTEM\",\"text\":\"" + escape(msg) + "\"}";
    }

    private static String escape(String s) {
        // 최소 이스케이프
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // 데모용 간단 파서(실전에서는 DTO + Jackson 권장)
    private String extractTextField(String json) {
        int i = json.indexOf("\"text\"");
        if (i < 0) return json;
        int c = json.indexOf(':', i);
        int q1 = json.indexOf('"', c + 1);
        int q2 = json.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return json;
        return json.substring(q1 + 1, q2);
    }

    // 서버→클라 텍스트 프레임 직렬화를 위한 간단 DTO
    public record Msg(String type, String text) {}
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
