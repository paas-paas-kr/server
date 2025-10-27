package com.chat.conversation.service;

import com.chat.conversation.dto.ConversationMessage;
import com.chat.conversation.dto.ConversationRoom;
import com.chat.conversation.dto.PageResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import com.google.api.gax.rpc.StatusCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class ConversationService {

    private final Firestore db;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    public ConversationService(Firestore db) {
        this.db = db;
    }

    private static final String MESSAGES = "conversationMessages";
    private static final String ROOMS = "conversationRooms";

    //size 개수만큼 채팅방 가져오기
    public PageResponseDto<ConversationRoom> getRoomsByToken(
        String userId, @Nullable String pageToken, int size
    ){
        // fetch = size +1을 주는 것은 다음 페이지 존재 여부 판단용 여분 한 건을 확보하려는 의도
        // 예를 들어, size= 20이면 21건을 읽어서, 21건이 오면 뒤에 더 있구나라고 판단한다.
        int fetch = Math.min(Math.max(1,size),100)+1;

        try{
            CollectionReference conversationsRef = db.collection(ROOMS);
            Query baseQuery = conversationsRef
                    .whereEqualTo("userId",userId)  // 특정 사용자의 문서만 가져온다.
                    .orderBy("lastMessageAt",Query.Direction.DESCENDING)  //최근순 정렬
                    // FieldPath는 문서 안의 특정 필드를 가리키는 경로 타입
                    // 문서 ID는 일반 필드가 아니라 Firestore가 내부적으로 들고 있는 특수 메타필드이기 때문에 이름이 아닌 전용 표기로 참조
                    // documentId는 랜덤으로 생성되기 때문에 시간순 정렬이 아니지만
                    // 동일한 시간값(lastMessageAt)들 사이에서 항상 같은 순서를 보장하려고 2차 정렬로 씀( 안정 정렬)
                    .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                    .limit(fetch);

            if(pageToken!=null && !pageToken.isBlank()){
                Cursor c = decodeToken(pageToken);
                baseQuery= baseQuery.startAfter(c.lastMessageAt(),c.docId());
            }
            // where/orderBy/limit등으로 만든 Query를 get()하면 그 시점에 쿼리에 부합하는 문서 집합을 한 번에 담아온다.
            // 쿼리 실행
            ApiFuture<QuerySnapshot> fut =baseQuery.get();

            // 쿼리 결과 묶음
            // QuerySnapshot은 내부에 여러 문서 스냅샷을 가지고 있고, 크기/비었는지/읽은 시간 같은 메타데이터도 있다.
            // snap.getDocuments(), snap.size(), snap.isEmpty()
            QuerySnapshot snap = fut.get();

            // 묶음 안의 각 문서
            List<QueryDocumentSnapshot> documents = snap.getDocuments();

            // documents.size()와 fetch가 같다면 최소 한개는 더 문서가 남아있다는 의미
            // documents.size()가 fetch보다 작다면 더이상 남아있는 문서가 없다는 의미
            boolean hasNext = documents.size() == fetch;

            if(hasNext){
                System.out.println(documents.get(documents.size()-1));
                documents = documents.subList(0, fetch-1); //사용자에게는 size개만 반환
            }

            List<ConversationRoom> rooms = new ArrayList<>(documents.size());
            for(QueryDocumentSnapshot d:documents){
                //문서의 필드들을 자바 객체(POJO)로 매핑
                // Firestore SDK가 문서의 구조화된 데이터(Map 형태)를 특정 클래스의 필드로 채워 놓는 것
                //객체로 바로 매핑: d.toObject(ConversationRoom.class)
                //맵으로 받기: Map<String,Object> m = d.getData();
                //개별 필드로: d.getString("title"), d.getTimestamp("lastMessageAt") 등
                ConversationRoom r= d.toObject(ConversationRoom.class);
                r.setId(d.getId());
                rooms.add(r);
            }

            String next= null;
            if(hasNext && !documents.isEmpty()){
                QueryDocumentSnapshot last = documents.get(documents.size()-1);
                Timestamp ts = last.getTimestamp("lastMessageAt");
                if(ts==null){
                    ts= Timestamp.MIN_VALUE;
                }
                next= encodeToken(ts, last.getId());
            }


            return new PageResponseDto<>(rooms, next, hasNext);

        }catch(InterruptedException e){
            throw new RuntimeException("목록 조회 중 인터럽트",e);
        }catch(Exception e){
            throw new RuntimeException("Firestore 목록 조회 실패",e);
        }
    }

    //size 개수만큼 채팅방 가져오기
    public PageResponseDto<ConversationMessage> getMessagesByToken(
            String userId, String roomId, @Nullable String pageToken, int size
    ){
        // fetch = size +1을 주는 것은 다음 페이지 존재 여부 판단용 여분 한 건을 확보하려는 의도
        // 예를 들어, size= 20이면 21건을 읽어서, 21건이 오면 뒤에 더 있구나라고 판단한다.
        int fetch = Math.min(Math.max(1,size),100)+1;

        try{
            CollectionReference conversationsRef = db.collection(ROOMS).document(roomId).collection(MESSAGES);
            Query baseQuery = conversationsRef
                    .orderBy("createdAt",Query.Direction.DESCENDING)  //최근순 정렬
                    .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING);

            if(pageToken!=null && !pageToken.isBlank()){
                Cursor c = decodeToken(pageToken);
                baseQuery= baseQuery.startAfter(c.lastMessageAt(),c.docId());
            }

            baseQuery= baseQuery.limit(fetch);

            ApiFuture<QuerySnapshot> fut =baseQuery.get();
            QuerySnapshot snap = fut.get();
            List<QueryDocumentSnapshot> documents = snap.getDocuments();

            boolean hasNext = documents.size() == fetch;

            if(hasNext){
                System.out.println(documents.get(documents.size()-1));
                documents = documents.subList(0, fetch-1); //사용자에게는 size개만 반환
            }

            List<ConversationMessage> messages = new ArrayList<>(documents.size());
            for(QueryDocumentSnapshot d:documents){
                ConversationMessage m= d.toObject(ConversationMessage.class);
                m.setId(d.getId());
                m.setRoomId(roomId);
                messages.add(m);
            }

            String next= null;
            if(hasNext && !documents.isEmpty()){
                QueryDocumentSnapshot last = documents.get(documents.size()-1);
                Timestamp ts = last.getTimestamp("createdAt");
                if(ts==null){
                    ts= Timestamp.MIN_VALUE;
                }
                next= encodeToken(ts, last.getId());
            }


            return new PageResponseDto<>(messages, next, hasNext);

        }catch(InterruptedException e){
            throw new RuntimeException("목록 조회 중 인터럽트",e);
        }catch(Exception e){
            throw new RuntimeException("Firestore 목록 조회 실패",e);
        }
    }


    //채팅방 생성
    public ConversationRoom createRoom(String userId) {

        // 네트워크 통신을 하지 않는다 (DB에 요청x)
        //Firestore 클라이언트 라이브러리(SDK)가 자체적으로 고유한 20자리 랜덤 ID를 생성한다.
        // 이 ID를 가진 빈 껍데기 주소, 즉 DocumentReference 객체를 만든다.
        DocumentReference docRef = db.collection(ROOMS).document();

        ConversationRoom room = new ConversationRoom();
        room.setTitle("새로운 대화");
        room.setUserId(userId);
        //@ServerTimestamp를 통해서 Firestore가 문서를 쓸 때 자동으로 현재 서버 시간을 해당 필드에 기록
        room.setLastMessageAt(null);
        room.setId(docRef.getId()); //확보된 ID를 객체에 설정

        try {
            // Firestore에 데이터를 쓰는 것은 네트워크를 통해 다른 서버에 요청하는 것
            // ApiFuture은 비동기적으로 작업 요청을 한 후 나중에 완료되면 결과를 담음
            // 실제 DB쓰기는 백그라운드 스레드에서 시작
            ApiFuture<WriteResult> future = docRef.set(room);

            // 쓰기가 완료될 때까지 여기서 대기(Blocking)
            future.get(); //이 시점에 InterruptedException 또는 ExecutionException

            // DB쓰기 성공이 확정된 후에 객체를 반환
            return room;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 복원
            throw new RuntimeException("채팅방 생성 실패(인터럽트)", e);
        } catch (ExecutionException e) {
            // .get() 실행 중 Firestore 쓰기 실패 (권한, 네트워크 등)
            throw new RuntimeException("채팅방 생성 실패", e);
        }

    }

    //채팅방 삭제
    public boolean deleteRoom(String roomId, String userId) {
        //(1) 메인 채팅방 문서 주소
        DocumentReference roomRef = db.collection(ROOMS).document(roomId);

        try {
            // 1) 존재/권한 확인 (여기서 없는 방이면 false 반환)
            DocumentSnapshot snap = roomRef.get().get();
            System.out.println(snap);
            if (!snap.exists()) {
                System.out.println("!23");
                return false; // 404로 매핑할 근거
            }

            // 2) 하위 메시지 삭제 (많으면 배치/스트리밍 삭제로)
            deleteAllMessages(roomId);

            // 3) 메인 문서 삭제
            roomRef.delete().get();

            return true;

        } catch (InterruptedException e) {
            //InterruptedException이 플래그를 지운다.
            // 플래그를 지워도 interrupt()를 다시 호출해 플래그를 복원해서
            // 이후의 코드가 잘못 계속 실행되는 것을 막아 정상적인 중단 흐름으로 이어진다.
            Thread.currentThread().interrupt();
            throw new RuntimeException("채팅방 삭제 중 인터럽트 발생", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("채팅방 삭제 중 DB 오류 발생", e);
        }

    }

    /**
     * 특정 채팅방의 모든 하위 메시지를 배치 방식으로 안전하게 삭제한다.
     * 메시지가 수백만 개라도 메모리 문제 없이 삭제할 수 있다.
     *
     * @param roomId
     */
    public void deleteAllMessages(String roomId) {
        // 1. 삭제할 하위 컬렉션의 참조를 가져온다.
        CollectionReference messages =
                db.collection(ROOMS).document(roomId).collection(MESSAGES);

        // 2. 페이지네이션 커서 역할을 할 변수이다.
        //    last는 이전 배치의 마지막 문서를 가리킨다.
        DocumentSnapshot last = null;

        // 3. Firestore 배치 쓰기는 한 번에 500개 작업이 최대
        int BATCH_SIZE = 500;

        // 4. 하위 컬렉션에 문서가 없을 때까지 무한 루프를 돈다.
        while (true) {
            //5. 이번 페이지에서 가져올 문서를 쿼리
            //   orderBy(FieldPath.documentId())는 페이지네이션의 안전성을 위한 필수 항목
            //   (매우 중요) documentId()를 기준으로 last의 documentId값보다 뒤에있는 documentId의 스냅샷들을 BATCH_SIZE개수만큼 가져옴
            Query q = messages.orderBy(FieldPath.documentId()).limit(BATCH_SIZE);

            // 6. last가 null이 아니면 (즉, 두번째 루프부터는)
            //    이전 루프의 마지막 문서 다음부터 쿼리를  시작한다.
            if (last != null) {
                //startAfter(x)는 커서를 의미
                // 정렬 기준 위에서 X이후부터 결과를 달라는 표식
                // 즉, documentId 정렬 상 last 바로 다음 문서부터 limit(BATCH_SIZE)개 주세요
                q = q.startAfter(last);
            }

            // 7. 쿼리를 실행하여 (비동기) 페이지 문서를 가져온다 (동기 대기)
            // 스냅샷은 복사본이고, 서버 자원에 붙어있는 참조가 아니다.
            // 따라서 스냅샷 자체를 지우는 게 아니라, 스냅샷이 가진 문서 참조를 이용해 서버의 문서를 지운다
            List<QueryDocumentSnapshot> page;
            try {
                // q: Query 객체 , q.get: ApiFuture<QuerySnapshot> q.get().get(): ApiFuture.get()= QuerySnapshot객체,
                // 마지막 .getDocuments(): QuerySnapshot#getDocuments(), 스냅샷 안의 결과를 List<QueryDocumentSnapshot>으로 꺼낸다
                page = q.get().get().getDocuments();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("메시지 조회 인터럽트", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("메시지 조회 실패", e);
            }

            // 8. [탈출 조건] 가져온 페이지가 비어있으면, 모든 문서가 삭제된 것이다.
            if (page.isEmpty()) {
                break;
            }

            // 9. 배치 쓰기 작업을 생성한다.
            //    배치 작업을 사용하면 500개의 삭제를 단일 작업으로 묶어
            //    네트워크 비용과 API 호출 횟수를 절약할 수 있다.
            WriteBatch batch = db.batch();

            // 10. 이번 페이지의 모든 문서를 배치 삭제 목록에 추가한다.
            for (QueryDocumentSnapshot doc : page) {
                batch.delete(doc.getReference());
            }

            // 11. 배치 작업을 DB에 커밋한다.
            //     .get()으로 완료될 때까지 동기식으로 대기한다.
            try {
                batch.commit().get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("메시지 삭제 인터럽트", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("메시지 배치 삭제 실패", e);
            }

            // 12. 다음 루프를 위해 last 커서를
            //     방금 처리한 페이지의 마지막 문서로 업데이트한다.
            //     페이지 문서를 삭제해도, last 스냅샷 안에는 이미 삭제한 문서들중 마지막 문서의 documentId(정렬 조건)를 가지고 있음
            //     다음 페이지를 요청할 때 startAfter(last)를 쓰면 SDK가 last에서 정렬 키 값들을 추출해서 서버에 보낸다.
            //     서버는 정렬상 이 값들보다 뒤부터를 반환한다.
            //     따라서 서버에서 문서가 삭제되었다고해도 정렬 키 값의 순서상의 위치만 중요하다.
            //     즉 커서는 값-기반 좌표라서 삭제 이후에도 동작한다
            last = page.get(page.size() - 1);
        }

    }

    //이건 컨트롤러에서 오는 요청이 아닌 webflux 핸들러에서 llm호출 후 오는 요청
    public Mono<ConversationMessage> createMessage(String question, String answer, String roomId) {
        DocumentReference ref = db.collection(ROOMS).document(String.valueOf(roomId)).collection(MESSAGES).document();
        String messageId = ref.getId();
        ConversationMessage message = ConversationMessage.builder()
                .question(question)
                .answer(answer)
                .id(messageId)
                .roomId(roomId)
                //createdAt은 @ServerTimestamp로 서버에서 채워짐
                .build();

        // Firestore의 ApiFuture를 리액터의 Mono로 수동 변환 (1/2)
        Mono<WriteResult> setMono = Mono.create(sink -> {
            // Mono.create(): "누군가 이 Mono를 구독(subscribe)하면, 이 람다(sink -> ...)를 실행시켜줘."
            // 'sink'는 Mono의 이벤트를(성공/에러/취소) 수동으로 발생시키는 객체입니다.

            // 1. Guava의 ApiFuture에 비동기 콜백을 등록합니다.
            //    ref.set(message)는 실제 쓰기 요청(ApiFuture)입니다.
            ApiFutures.addCallback(ref.set(message), new ApiFutureCallback<WriteResult>() {

                // 2. (실패 시) ApiFuture 작업이 실패하면(네트워크, 권한 등) 이 메서드가 호출됩니다.
                @Override
                public void onFailure(Throwable t) {
                    // 'sink'를 통해 Mono 스트림에 'error' 신호를 보냅니다.
                    sink.error(t);
                }

                // 3. (성공 시) ApiFuture 작업이 성공하면 이 메서드가 호출됩니다.
                @Override
                public void onSuccess(WriteResult wr) {
                    // 'sink'를 통해 Mono 스트림에 'success' 신호와 결과값(wr)을 보냅니다.
                    sink.success(wr);
                }
            }, MoreExecutors.directExecutor()); // 4. [중요] 콜백을 즉시 실행합니다.
            // Firestore의 I/O 스레드(작업을 완료한 스레드)에서
            // 별도 스레드 전환 없이 바로 콜백(onFailure/onSuccess)을 실행합니다.
            // 불필요한 스레드 풀링을 막아 매우 효율적입니다.
        });

        // Firestore의 ApiFuture를 리액터의 Mono로 수동 변환 (2/2)
        Mono<DocumentSnapshot> getMono = Mono.create(sink -> {
            // 'setMono'와 구조는 완전히 동일합니다.
            // ref.get()이라는 비동기 작업을 감쌉니다.
            ApiFutures.addCallback(ref.get(), new ApiFutureCallback<DocumentSnapshot>() {

                // (실패 시) 'get' 작업이 실패하면 Mono에 'error' 신호를 보냅니다.
                @Override
                public void onFailure(Throwable t) {
                    sink.error(t);
                }

                // (성공 시) 'get' 작업이 성공하면 Mono에 'success' 신호와 결과값(DocumentSnapshot)을 보냅니다.
                @Override
                public void onSuccess(DocumentSnapshot snap) {
                    sink.success(snap);
                }

            }, MoreExecutors.directExecutor());
        });

        // 두 Mono를 순차적으로 조합하여 최종 결과를 만듭니다.
        return setMono // 1. 'setMono'를 먼저 실행(구독)합니다.
                .then(getMono) // 2. 'setMono'가 성공적으로 완료되면(onSuccess),
                //    그 결과(WriteResult)는 무시하고, 'getMono'를 실행(구독)합니다.
                //    'set'이 끝나야 'get'을 실행하는 순서가 보장됩니다.

                .map(snap -> { // 3. 'getMono'가 성공적으로 완료되면,
                    //    결과값(DocumentSnapshot)을 받아 이 'map' 함수를 실행합니다.

                    // 4. [방어 코드 1] .set() 직후 .get()을 했는데 문서가 없는 예외적인 경우,
                    //    서버 시간을 못 받았으니 일단 원본 객체라도 반환합니다.
                    if (!snap.exists()) return message;

                    // 5. 스냅샷을 자바 POJO(ConversationMessage)로 변환합니다.
                    //    이때 @ServerTimestamp 필드가 서버 시간으로 채워집니다.
                    ConversationMessage cm = snap.toObject(ConversationMessage.class);

                    // 6. [방어 코드 2] 객체 변환이 (알 수 없는 이유로) 실패한 경우, 원본 객체를 반환합니다.
                    if (cm == null) return message;

                    // 7. 반환할 객체에 ID와 RoomID를 확실하게 설정합니다.
                    //    (@DocumentId 어노테이션이 있어도, 명시적으로 다시 설정하는 것이 안전합니다.)
                    cm.setId(snap.getId());
                    cm.setRoomId(roomId); // (이 값은 이미 message 객체에 있었을 수 있지만, 확실하게 덮어씁니다)

                    return cm; // 8. 서버 시간이 적용된 최종 객체를 반환합니다.
                })
                .timeout(java.time.Duration.ofSeconds(5)) // 9. [안정성] 'set'과 'get'을 합친 전체 작업이 3초를 초과하면
                //    강제로 'TimeoutException'을 발생시키고 중단합니다.
                .retry(2)
                .onErrorResume(e ->
                {
                    //실패한 메시지 스케줄러로 다시 실행하는 메서드 만들기
                    //failedMessageQueue.send(message);
                    return Mono.just(message);
                }); // 10. [Fire-and-Forget 구현]
        //    위 1~9번 체인 중 *어디에서든* 에러가 발생하면
        //    (set 실패, get 실패, map 실패, timeout 등)
        //    스트림을 중단(error)시키지 말고,
        //    대신 원본 'message' 객체를 반환하는 '성공' Mono로 바꿔치기합니다.
        //    -> WebSocket 핸들러의 .subscribe() 에러 콜백이 호출되지 않습니다.
    }

    private static String encodeToken(Timestamp lastMessageAT, String docId){
        try{
            TokenPayload p = new TokenPayload(
                lastMessageAT.getSeconds(),
                lastMessageAT.getNanos(),
                docId
            );
            byte[] json = MAPPER.writeValueAsBytes(p);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        }catch(Exception e){
            throw new IllegalArgumentException("페이지 토큰 인코딩 실패",e);
        }
    }

    private static Cursor decodeToken(String token){
        try{
            byte[] json = Base64.getUrlDecoder().decode(token.getBytes(StandardCharsets.UTF_8));
            TokenPayload p = MAPPER.readValue(json, TokenPayload.class);
            Timestamp ts = Timestamp.ofTimeSecondsAndNanos(p.seconds(),p.nanos());
            return new Cursor(ts,p.docId());
        }catch  (Exception e){
            throw new IllegalArgumentException("잘못된 페이지 토큰",e);
        }
    }
    /**
     * 페이징에 사용할 커서 값: lastMessageAt(1차 키), docId(2차 키)
     *
     * Firestore가 문서와 POJO 매핑(toObject/fromObject)에선 record를 지원하지 않는다.
     * 보통 public 무인자 생성자 + getter/setter가 있는 클래스를 요구한다.
     * 그래서 ConversationRoom같은 컬렉션에 저장/조회되는 엔티티는 record로 만들면 안 된다.
     *
     * 하지만 Cursor/TokenPayload record는 Firestore 문서 매핑에 쓰이지 않고, 직접 Base64,JSON으로 인코딩, 디코딩할 때만 쓰는 내부 DTO이다.
     */
    private record Cursor(Timestamp lastMessageAt, String docId) {}

    // 토큰 내부 직렬화 페이로드(JSON) 외부 노출 금지
    private record TokenPayload (long seconds, int nanos, String docId){
    }
}
