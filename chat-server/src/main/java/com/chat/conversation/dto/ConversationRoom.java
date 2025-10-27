package com.chat.conversation.dto;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class ConversationRoom {
    @DocumentId
    private String id;

    private String userId;
    private String title;

    @ServerTimestamp
    private Timestamp lastMessageAt;
}
