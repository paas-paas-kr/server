package com.chat.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsOutbound {
    private String type;
    private String text;
    private Long ts;

    public static WsOutbound system(String text) {
        var o = new WsOutbound();
        o.type = "SYSTEM";
        o.text = text;
        return o;
    }

    public static WsOutbound pong(long ts) {
        var o = new WsOutbound();
        o.type = "PONG";
        o.ts = ts;
        return o;
    }
}