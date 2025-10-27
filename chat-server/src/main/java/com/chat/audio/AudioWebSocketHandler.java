package com.chat.audio;

import com.chat.audio.model.AudioChunk;
import com.chat.audio.model.AudioMeta;
import com.chat.common.json.JsonUtils;
import com.chat.common.ws.SessionRegistry;
import com.chat.common.ws.WsEmitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudioWebSocketHandler implements WebSocketHandler {

    private final SessionRegistry registry;
    private final AudioProcessor processor;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        final String sid = session.getId();
        final WsEmitter emitter = registry.createEmitter(sid, session);
        final long startedAtNanos = System.nanoTime();
        final AudioAggregator aggregator = new AudioAggregator();

        var inbound = session.receive()
                .flatMap(msg -> {


                    if (msg.getType() == WebSocketMessage.Type.TEXT) {
                        // 수신
                        var text = msg.getPayloadAsText();
                        System.out.println(text);
                        // FINISH: 짧은 디바운스 후 병합 → 처리 완료 시 소켓 종료
                        if (text.startsWith("{") && text.contains("\"type\":\"FINISH\"")) {
                            return Mono.defer(() -> {
                                try {
                                    if (aggregator.meta == null || isBlank(aggregator.meta.getLang())) {
                                        log.warn("[AUDIO:{}] FINISH without lang/meta", sid);
                                        emitter.emitText(system("언어를 선택하세요."));
                                        emitter.complete();
                                        return Mono.empty();
                                    }
                                    // 늦게 도착하는 마지막 청크 수용
                                    return Mono.delay(Duration.ofMillis(180))
                                            .then(Mono.fromCallable(aggregator::merge))
                                            .flatMap(merged -> {
                                                int len = merged == null ? -1 : merged.length;
                                                log.info("[AUDIO:{}] merged {}B on FINISH", sid, len);

                                                if (merged == null || merged.length == 0) {
                                                    emitter.emitText(system("녹음된 오디오가 없습니다."));
                                                    // cleanup & complete
                                                    processor.complete(sid);
                                                    registry.cleanup(sid);
                                                    emitter.complete();
                                                    return Mono.empty();
                                                }
                                                String mime = (aggregator.meta != null ? aggregator.meta.getMimeType() : null);

                                                // 처리 완료/오류 시점에서만 소켓 종료
                                                return processor.processFinal(sid, merged, mime, emitter)
                                                        .doOnError(e -> {
                                                            log.error("[AUDIO:{}] process failed on FINISH", sid, e);
                                                            emitter.emitText(system("오디오 처리 오류: " + e.getMessage()));
                                                        })
                                                        .doFinally(s -> {
                                                            processor.complete(sid);
                                                            registry.cleanup(sid);
                                                            emitter.complete();
                                                        });
                                            });
                                } catch (Exception e) {
                                    log.error("[AUDIO:{}] process failed on FINISH-enter", sid, e);
                                    emitter.emitText(system("오디오 처리 오류: " + e.getMessage()));
                                    emitter.complete();
                                    return Mono.empty();
                                }
                            });
                        }

                        // START 메타
                        try {
                            AudioMeta meta = JsonUtils.fromJson(text, AudioMeta.class);
                            aggregator.setMeta(meta);
                            emitter.setAttribute("audioMeta", meta);
                            emitter.setAttribute("roomId",meta.getRoomId());
                            processor.onMeta(sid, meta);
                            log.info("[AUDIO:{}] meta: {}", sid, meta);
                        } catch (Exception ignore) {
                            log.debug("[AUDIO:{}] text ignored: {}", sid, text);
                        }
                        return Mono.empty();
                    }

                    /**
                     *  브라우저 MediaRecorder가 마이크 스트림을 주기적으로 잘라 Blob 청크를 만들어 준다.
                     *  그 주기를 정하는게 recorder.start(timesliceMS)
                     *  // [ 4바이트(seq, big-endian) | payload(body) ] 포맷의 바이트 배열 생성
                     *  const out  = new Uint8Array(4 + body.byteLength);
                     *  const view = new DataView(out.buffer);
                     *  view.setUint32(0, seq);               // 서버의 toIntBE(0)과 호환되는 big-endian
                     *  out.set(new Uint8Array(body), 4);     // 뒤에 오디오 바이트 부착
                     *  wsAudio.send(out);
                     *  sentChunks++;
                     *
                     * BINARY 프레임 예시:
                     * 클라이언트에서 보내는 바이너리 프레임 페이로드 모양
                     * [ 00 00 00 2A | <오디오 바이트...>(가변) ]
                     * 앞 4바이트: 시퀀스 번호
                     * -> 네트워크에서 프레임 순서 바뀌거나 일부 늦게 도착해도, 서버가 seq로 정렬해서 시간 순으로 복원하기 위해서
                     * 뒤쪽: 오디오 바이트(청크)
                     *
                     */
                    if (msg.getType() == WebSocketMessage.Type.BINARY) {
                        return Mono.fromSupplier(() -> {
                                    var db = msg.getPayload();
                                    System.out.println(db);
                                    byte[] bytes = new byte[db.readableByteCount()];
                                    db.read(bytes);
                                    return bytes;
                                })
                                .doOnNext(bytes -> {
                                    if (bytes.length < 4) {
                                        log.warn("[AUDIO:{}] invalid chunk(<4B)", sid);
                                        return;
                                    }
                                    int seq = toIntBE(bytes, 0); // 클라 DataView.setUint32(0, seq)와 호환
                                    byte[] payload = Arrays.copyOfRange(bytes, 4, bytes.length);

                                    aggregator.add(seq, payload);
                                    log.info("[AUDIO:{}] recv seq={} payload={}B", sid, seq, payload.length);

                                    long tsMs = (System.nanoTime() - startedAtNanos) / 1_000_000L;
                                    processor.onChunk(sid, new AudioChunk(payload, tsMs));
                                })
                                .then();
                    }

                    return Mono.empty();
                })
                .doFinally(sig -> {
                            // 소켓 비정상 종료 시, 아직 병합 안 했으면 마지막 시도 (디바운스 없이 즉시)
                            try {
                                if (!aggregator.isClosed()) {
                                    if (aggregator.meta == null || isBlank(aggregator.meta.getLang())) {
                                        log.warn("[AUDIO:{}] finally without lang/meta, skip", sid);
                                    } else {
                                        byte[] merged = aggregator.merge();
                                        if (merged != null && merged.length > 0) {
                                            String mime = (aggregator.meta != null ? aggregator.meta.getMimeType() : null);
                                            processor.processFinal(sid, merged, mime, emitter)
                                                    .doOnSubscribe(s -> System.out.println("processFinal 구독"))
                                                    .doOnSuccess(v -> System.out.println("processFinal 성공"))
                                                    .then(Mono.defer(session::close))
                                                    .doFinally(s -> {
                                                        processor.complete(sid);
                                                        registry.cleanup(sid);
                                                        emitter.complete();
                                                    })
                                                    .subscribe();
                                        } else {
                                            processor.complete(sid);
                                            registry.cleanup(sid);
                                            emitter.complete();
                                        }
                                    }
                                } else {
//                                    // 이미 FINISH 경로에서 정리됨
//                                    processor.complete(sid);
//                                    registry.cleanup(sid);
//                                    emitter.complete();
                                }
                            } catch (Exception e) {
                                log.error("[AUDIO:{}] process failed on finally", sid, e);
                                processor.complete(sid);
                                registry.cleanup(sid);
                                emitter.complete();
                            }
                        }
                );

        var outbound = session.send(emitter.flux());
        return Mono.when(inbound, outbound);
    }

    // -------- helpers --------

    private static int toIntBE(byte[] a, int off) {
        return ((a[off] & 0xFF) << 24)
                | ((a[off + 1] & 0xFF) << 16)
                | ((a[off + 2] & 0xFF) << 8)
                | (a[off + 3] & 0xFF);
    }

    private static String system(String msg) {
        return "{\"type\":\"SYSTEM\",\"text\":\"" + msg.replace("\"", "\\\"") + "\"}";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * seq 기준 정렬/병합기.
     * MediaRecorder 조각은 컨테이너 레벨 완전 보장되진 않으므로, 최종 파일은 서버에서 remux/변환 권장.
     */
    static final class AudioAggregator {
        private final ConcurrentSkipListMap<Integer, byte[]> ordered = new ConcurrentSkipListMap<>();
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final LongAdder totalBytes = new LongAdder();
        volatile AudioMeta meta;

        void setMeta(AudioMeta meta) {
            this.meta = meta;
        }

        void add(int seq, byte[] payload) {
            if (closed.get()) return;
            ordered.compute(seq, (k, v) -> {
                if (v == null) {
                    totalBytes.add(payload.length);
                    return payload;
                }
                // 중복 seq 방어: 최초 것만 유지
                return v;
            });
        }

        boolean isClosed() {
            return closed.get();
        }

        byte[] merge() {
            if (!closed.compareAndSet(false, true)) return new byte[0];
            long size = totalBytes.sum();
            if (size <= 0) return new byte[0];
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) Math.min(size, Integer.MAX_VALUE));
            try {
                for (var e : ordered.entrySet()) bos.write(e.getValue());
                return bos.toByteArray();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                ordered.clear();
            }
        }
    }

}
