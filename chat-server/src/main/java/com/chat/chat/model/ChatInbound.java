package com.chat.chat.model;

import com.chat.common.Lang;
import com.chat.common.constants.MessageType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatInbound {
    private MessageType type; // CHAT/START/PING ...
    private String text;      //
    private String userId;
    private String lang;

}