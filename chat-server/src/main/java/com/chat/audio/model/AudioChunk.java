package com.chat.audio.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AudioChunk {
    private byte[] bytes;
    private long timestampMs;

    public AudioChunk(byte[] bytes) {
        this(bytes, System.currentTimeMillis());
    }
}