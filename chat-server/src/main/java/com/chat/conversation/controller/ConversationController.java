package com.chat.conversation.controller;

import com.chat.conversation.dto.ConversationMessage;
import com.chat.conversation.dto.ConversationRoom;
import com.chat.conversation.dto.PageResponseDto;
import com.chat.conversation.service.ConversationService;
import com.common.security.GatewayUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/conversations")
public class ConversationController {
    private final ConversationService conversationService;

    @PostMapping
    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 만듭니다.")
    public Mono<ConversationRoom> createConversation(
            @AuthenticationPrincipal GatewayUserDetails principal,
            @RequestHeader(value = "X-User-Id", required = false) String userIdFromHeader
    ) {
        System.out.println("채팅방 생성 컨트롤러 도착");
        String userId = resolveUserId(principal, userIdFromHeader);

        return Mono.fromSupplier(() -> conversationService.createRoom(userId));
    }

    @DeleteMapping
    @Operation(
            summary = "채팅방 삭제",
            description = "채팅방 ID와 유저 ID 받아서 채팅방 삭제"
    )
    public Mono<ResponseEntity<Void>> deleteConversation(
            @AuthenticationPrincipal GatewayUserDetails principal,
            @RequestHeader(value = "X-User-id", required = false) String userIdFromHeader,
            @RequestParam(required = true) String roomId
    ) {
        System.out.println("채팅방 삭제 컨트롤러 도착");
        String userId = resolveUserId(principal, userIdFromHeader);

        //Mono.fromRunnable()
        // -> 동기(블로킹) 작업을 Mono로 감싸서 리액티브 체인에 올린다.
        // -> 반환값이 없고, 단순 실행만 하는 작업에 적합.
        return Mono.fromCallable(() -> conversationService.deleteRoom(roomId, userId)) // blocking
                // .subscribeOn(Schedulers.boundedElastic())
                //  → "boundedElastic" 스케줄러는 **블로킹 I/O용 스레드풀**.
                //     (파일, DB, 외부 API, Firestore 같은 블로킹 호출에 안전)
                //  → Reactor의 기본 스케줄러(EventLoop)는 논블로킹 전용이라
                //     블로킹 메서드를 그 위에서 실행하면 이벤트 루프가 멈춤 → 전체 성능 저하.
                //  → boundedElastic은 "필요 시 새로운 스레드 생성 + 최대 제한 있음(기본 10 * CPU코어)"으로,
                //     블로킹 코드가 병렬로 안전하게 돌아가도록 보장한다.
                .subscribeOn(Schedulers.boundedElastic())
                .map(deleted-> deleted
                        ?ResponseEntity.noContent().build()
                        :ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                   }

    @GetMapping("rooms")
    @Operation(
            summary = "채팅방 목록 가져오기",
            description = "특정 유저의 채팅방 목록 N개를 페이지네이션으로 조회합니다."
    )
    public Mono<PageResponseDto<ConversationRoom>> getConversationsRooms(
            @RequestHeader(value = "X-User-id", required = false) String userIdFromHeader,
            @AuthenticationPrincipal GatewayUserDetails principal,
            @RequestParam(name = "pageToken", required = false) String pageToken,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        String userId = resolveUserId(principal, userIdFromHeader);
        return Mono.fromSupplier(() -> conversationService.getRoomsByToken(userId, pageToken, size));
    }

    @GetMapping("room/{roomId}/messages")
    @Operation(
            summary = "채팅 목록 가져오기",
            description = "특정 채팅방의 메시지 N개를 페이지네이션으로 조회합니다."
    )
    public Mono<PageResponseDto<ConversationMessage>> getConversationsMessages(
            @RequestHeader(value = "X-User-id", required = false) String userIdFromHeader,
            @AuthenticationPrincipal GatewayUserDetails principal,
            @RequestParam(name = "pageToken", required = false) String pageToken,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @PathVariable(value = "roomId") String roomId,
            ServerWebExchange exchange
    ) {
        String userId = resolveUserId(principal, userIdFromHeader);
        return Mono.fromSupplier(()->conversationService.getMessagesByToken(userId, roomId, pageToken, size));
    }

    private String resolveUserId(@Nullable GatewayUserDetails principal, @Nullable String headerUserId) {
        if (principal != null && principal.getUserId() != null) {
            return String.valueOf(principal.getUserId());
        }
        if (headerUserId != null && !headerUserId.isBlank()) {
            return headerUserId.trim();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 식별자 누락");
    }

}
