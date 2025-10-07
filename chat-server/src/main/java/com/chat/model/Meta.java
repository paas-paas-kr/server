package com.chat.model;

import lombok.Data;

/** 이번 단계에선 사용하지 않지만 구조 유지 */
@Data
public class Meta {
    private Boolean tts;
    private String locale;
    private String tone;
    private String culture;
}
