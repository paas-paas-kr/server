package com.chat.model;

import lombok.Data;

import lombok.Data;

/** 이번 단계에선 사용하지 않지만 구조 유지 */
@Data
public class SessionState {
    private String userId;
    private String lang;
    private String format;
    private Integer sampleRate;
}