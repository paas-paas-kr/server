package com.chat.audio.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * byte[] bytes: 실제 오디오 데이터를 담는 배열
 * -> 오디오나 비디오 같은 미디어 데이터는 보통 바이트의 연속으로 표현되기 때문에 byte[] 타입을 사용
 */

@Data
@AllArgsConstructor
public class AudioChunk {
    private byte[] bytes;
    private long tsMs;

    // 오디오 데이터만 받아서, System.currentTimeMillis()를 통해 객체가 만들어지는 바로 그 순간의 시간을 타임스탬프로 자동 설정
    public AudioChunk(byte[] bytes) {
        this(bytes, System.currentTimeMillis());
    }
}