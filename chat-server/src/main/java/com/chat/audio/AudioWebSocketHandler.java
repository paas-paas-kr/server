package com.chat.audio;

import com.chat.audio.model.AudioChunk;
import com.chat.audio.model.AudioMeta;
import com.chat.common.json.JsonUtils;
import com.chat.common.ws.SessionRegistry;
import com.chat.common.ws.WsEmitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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

        // 세션별 오디오 누적기
        final AudioAggregator aggregator = new AudioAggregator();

        var inbound = session.receive()
                .flatMap(msg -> {
                    if (msg.getType() == WebSocketMessage.Type.TEXT) {
                        var text = msg.getPayloadAsText();

                        // FINISH 요청: 즉시 병합 → 단 한 번 BINARY emit → complete
                        if (text.startsWith("{") && text.contains("\"type\":\"FINISH\"")) {
                            return Mono.fromRunnable(() -> {
                                try {
                                    byte[] merged = aggregator.merge();
                                    if (merged != null && merged.length > 0) {
                                        emitter.emitBinary(withSeqHeader(1, merged));
                                        log.info("[AUDIO:{}] send merged {}B (once)", sid, merged.length);
                                    } else {
                                        log.info("[AUDIO:{}] no audio to send on FINISH", sid);
                                    }
                                } catch (Exception e) {
                                    log.error("[AUDIO:{}] merge/send failed on FINISH", sid, e);
                                } finally {
                                    // 서버가 종료 주도 (클라가 stop()에서 close 진행)
                                    emitter.complete();
                                }
                            });
                        }

                        // 메타(초기 1회)
                        try {
                            var meta = JsonUtils.fromJson(text, AudioMeta.class);
                            aggregator.setMeta(meta);
                            processor.onMeta(sid, meta);
                            log.info("[AUDIO:{}] meta: {}", sid, meta);
                        } catch (Exception ignore) {
                            // FINISH가 아니고, AudioMeta도 아니면 무시
                            log.debug("[AUDIO:{}] text ignored: {}", sid, text);
                        }
                        return Mono.empty();
                    }

                    if (msg.getType() == WebSocketMessage.Type.BINARY) {
                        return Mono.fromSupplier(() -> {
                                    var db = msg.getPayload();
                                    byte[] bytes = new byte[db.readableByteCount()];
                                    db.read(bytes);
                                    return bytes;
                                })
                                .doOnNext(bytes -> {
                                    if (bytes.length < 4) {
                                        log.warn("[AUDIO:{}] invalid chunk(<4B)", sid);
                                        return;
                                    }
                                    int seq = toIntBE(bytes, 0);
                                    byte[] payload = Arrays.copyOfRange(bytes, 4, bytes.length);

                                    aggregator.add(seq, payload);
                                    log.info("[AUDIO:{}] recv seq={} payload={}B", sid, seq, payload.length);

                                    // (선택) STT 파이프라인
                                    long tsMs = (System.nanoTime() - startedAtNanos) / 1_000_000L;
                                    processor.onChunk(sid, new AudioChunk(payload, tsMs));
                                })
                                .then();
                    }

                    return Mono.empty();
                })
                .doFinally(sig -> {
                    try {
                        // 소켓이 예기치 않게 끊긴 경우에도, 아직 전송 안 했으면 마지막 시도
                        if (!aggregator.isClosed()) {
                            byte[] merged = aggregator.merge();
                            if (merged != null && merged.length > 0) {
                                emitter.emitBinary(withSeqHeader(1, merged));
                                log.info("[AUDIO:{}] send merged {}B on finally", sid, merged.length);
                            }
                        }
                    } catch (Exception e) {
                        log.error("[AUDIO:{}] merge/send failed on finally", sid, e);
                    } finally {
                        processor.complete(sid);
                        registry.cleanup(sid);
                    }
                });

        var outbound = session.send(emitter.flux());
        return Mono.when(inbound, outbound);
    }

    // -------- helpers --------

    private static int toIntBE(byte[] a, int off) {
        return ((a[off] & 0xFF) << 24)
                | ((a[off + 1] & 0xFF) << 16)
                | ((a[off + 2] & 0xFF) << 8)
                |  (a[off + 3] & 0xFF);
    }

    private static byte[] withSeqHeader(int seq, byte[] payload) {
        byte[] out = new byte[4 + payload.length];
        out[0] = (byte)((seq >>> 24) & 0xFF);
        out[1] = (byte)((seq >>> 16) & 0xFF);
        out[2] = (byte)((seq >>> 8) & 0xFF);
        out[3] = (byte)(seq & 0xFF);
        System.arraycopy(payload, 0, out, 4, payload.length);
        return out;
    }

    /**
     * seq 기준 정렬/병합기.
     * 주의: MediaRecorder(WebM/Opus) 조각 단순 연결은 컨테이너 관점 완전 보장을 하진 않음.
     * 다운로드/1회 재생 용도엔 보통 충분하지만, 확정 파일은 remux 권장.
     */
    static final class AudioAggregator {
        private final ConcurrentSkipListMap<Integer, byte[]> ordered = new ConcurrentSkipListMap<>();
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final LongAdder totalBytes = new LongAdder();
        private volatile AudioMeta meta;

        void setMeta(AudioMeta meta) { this.meta = meta; }

        void add(int seq, byte[] payload) {
            if (closed.get()) return;
            ordered.computeIfAbsent(seq, s -> {
                totalBytes.add(payload.length);
                return payload;
            });
        }

        boolean isClosed() { return closed.get(); }

        byte[] merge() {
            if (!closed.compareAndSet(false, true)) return new byte[0];
            long size = totalBytes.sum();
            if (size <= 0) return new byte[0];

            ByteArrayOutputStream bos = new ByteArrayOutputStream((int)Math.min(size, Integer.MAX_VALUE));
            try {
                for (var e : ordered.entrySet()) {
                    bos.write(e.getValue());
                }
                return bos.toByteArray();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                ordered.clear();
            }
        }
    }
}
