package com.chat.conversation.dto;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.IgnoreExtraProperties;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@IgnoreExtraProperties //Firestore가 데이터베이스의 문서를 java객체(POJO)로 변환할 때, 객체에 없는 필드는 조용히 무시하도록 지시하는 어노테이션
public class ConversationMessage{
    @DocumentId
    private String id;
    private String question;
    private String answer;
    private String roomId;
    @ServerTimestamp
    private Timestamp createdAt;

}