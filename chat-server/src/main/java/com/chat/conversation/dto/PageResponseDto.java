package com.chat.conversation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@ToString
public class PageResponseDto<T> {
    private List<T> items;
    private String nextPageToken;
    private boolean hasNext;

    public PageResponseDto(List<T> items, String nextPageToken, boolean hasNext) {
        this.items = items;
        this.nextPageToken = nextPageToken;
        this.hasNext = hasNext;
    }
}
