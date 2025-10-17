package com.chat.audio;



import com.chat.audio.model.AudioChunk;
import com.chat.audio.model.AudioMeta;
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
    private final AudioTranscoder transcoder;            // webm/opus → WAV 변환용
    private final NaverSttClient sttClient;     // 네이버 STT REST 호출
    private final AppProperties props;                   // ffmpeg 경로 등 접근 가능
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
     * sessionId: 현재 처리 중인 작업(세션)을 구분하기 위한 고유한 ID
     * mergedBytes: byte 배열은 오디오 파일과 같은 바이너리(이진) 데이터를 담는다
     * mimeType: 데이터의 종류를 알려주는 정보(ex: audio/webm 처럼 오디오 파일의 포맷을 나타냄)
     * WsEmitter emitter: 클라이언트와의 웹소켓 연결 통로
     * -> 이 객체를 통해 서버가 클라이언트에게 메시지를 실시간으로 보낼 수 있다.
     *
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

        /**
         * 실제 비동기 작업 파이프라인 시작
         * Mono.fromCallable(...)은 시간이 걸리거나 현재 스레드를 차단할 수 있는 일반 코드를
         * 비동기 Mono 작업으로 감싸주는 역할을 한다.
         *
         * .subscribeOn(...): 바로 위에서 정의한 오디오 변환 작업을 어떤 스레드에서 실행할지 지정
         * -> Schedulers.boundedElastic(): 블로킹 작업 전용으로 마련된 별도의 스레드 풀
         * ->
         */
        return Mono.fromCallable(() -> {
                    // mimeType을 보고 STT 서버가 이해할 수 있는 오디오 포맷으로 변환해야 하는지 검사
                    // 변환이 필요하다면, transcoder를 사용해 mergedBytes 데이터를 PCM WAV 포맷으로 변환한 결과를 반환
                    if (needsTranscode(mimeType)) {
                        return transcoder.webmOpusToPcmWav16kMono(mergedBytes);
                    }
                    return mergedBytes;
                })
                .subscribeOn(Schedulers.boundedElastic())   // ffmpeg 호출은 블로킹 → 분리
                .flatMap(wav -> sttClient.transcribe(wav)) // 네이버 STT 호출
                .map(this::extractTextField)                 // {"text":"..."} → "..."
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

    @Nullable
    private String extractTextField(String json) {
        try {
            System.out.println(json);
            //ObjectMapper로 전체 JSON을 파싱해 루트 노드를 얻는다.
            //readTree는 트리 전체를 메모리에 구성한다.
            JsonNode root = MAPPER.readTree(json);
            // 최상위 "text"만 뽑을 때
            JsonNode text = root.path("text");
            if(text.isMissingNode()||text.isNull()||!text.isTextual()){
                return null; //문자열이 아니면 버림
            }
            return text.asText(); //여기서는 확실히 문자열
         } catch (Exception e) {
            log.warn("Bad STT JSON",e);
            return null;
        }
    }


    // 서버→클라 텍스트 프레임 직렬화를 위한 간단 DTO
    public record Msg(String type, String text) {}
}

