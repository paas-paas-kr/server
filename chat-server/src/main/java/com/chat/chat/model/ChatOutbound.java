package com.chat.chat.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatOutbound {
    private String type;
    private String text;
    private Long ts;

    public static ChatOutbound system(String text) {
        var o = new ChatOutbound();
        o.type = "SYSTEM";
        o.text = text;
        return o;
    }

    public static ChatOutbound pong(long ts) {
        var o = new ChatOutbound();
        o.type = "PONG";
        o.ts = ts;
        return o;
    }
}