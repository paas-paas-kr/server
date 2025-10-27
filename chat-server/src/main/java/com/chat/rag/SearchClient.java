package com.chat.rag;

import com.chat.rag.model.Citation;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SearchClient {
    Mono<List<Citation>> search(String query, int topK);
}
