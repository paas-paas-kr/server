package com.chat.stt.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TranscriptSegment {
    private boolean isFinal;
    private String text;
    private Long startMs;
    private Long endMs;
}
