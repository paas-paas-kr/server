package com.chat.stt.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SttRequest {
    private byte[] audio;           // 간단화를 위해 배치 전송용
    private String mimeType;
    private Integer sampleRate;
}

