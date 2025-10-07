package com.chat.util;

import com.chat.model.WsInbound;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    public static WsInbound fromJsonInbound(String json) {
        try { return MAPPER.readValue(json, WsInbound.class); }
        catch (Exception e) { throw new RuntimeException("Invalid inbound JSON: " + e.getMessage(), e); }
    }

    public static String toJson(Object obj) {
        try { return MAPPER.writeValueAsString(obj); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }
}