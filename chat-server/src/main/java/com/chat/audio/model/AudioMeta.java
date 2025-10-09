package com.chat.audio.model;

import lombok.Data;

/**
 *
 */
@Data
public class AudioMeta {
    private String mimeType;
    private Integer sampleRate;
    private Integer channels;
}
