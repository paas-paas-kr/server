package com.chat.common.json;

import com.chat.chat.model.ChatInbound;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    public static ChatInbound fromJsonInbound(String json) {
        try { return MAPPER.readValue(json, ChatInbound.class); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException(e); }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try { return MAPPER.readValue(json, type); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException(e); }
    }
    public static String toJson(Object o) {
        try { return MAPPER.writeValueAsString(o); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException(e); }
    }
}