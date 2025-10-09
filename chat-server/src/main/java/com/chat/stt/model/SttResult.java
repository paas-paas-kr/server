package com.chat.stt.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SttResult {
    private List<TranscriptSegment> segments;
}
