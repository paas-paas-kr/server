package com.chat.stt;

import reactor.core.publisher.Mono;

public interface SttClient {
    Mono<String> transcribe(byte[] audio);
}
