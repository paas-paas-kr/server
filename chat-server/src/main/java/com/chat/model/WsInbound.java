package com.chat.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsInbound {
    private MessageType type; // CHAT/START/PING ...
    private String text;      // 이번 목표: 채팅 텍스트
    private Integer seq;      // (미사용)
    private String format;    // (미사용)
    private Integer sampleRate; // (미사용)
    private Meta meta;        // (미사용)
}