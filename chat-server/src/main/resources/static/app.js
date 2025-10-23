// --- 필수 라이브러리 ---
// 이 코드가 작동하려면 HTML 파일에 Marked.js 라이브러리가 포함되어 있어야 한다.
// (Marked.js: Markdown 텍스트를 HTML로 변환해주는 라이브러리)
// 예: <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>

// ---------- DOM ----------
// 간단한 헬퍼: id로 DOM 요소 가져온다.
const $ = (id) => document.getElementById(id);

// 언어/타깃 언어 선택 셀렉트 박스
const langSel        = $('lang');

// Chat(텍스트 대화) 관련 요소
const wsUrlChatInp   = $('ws-url-chat');        // 채팅용 WS URL 입력
const btnChatConn    = $('btn-chat-connect');   // 채팅 연결 버튼
const btnChatDisc    = $('btn-chat-disconnect');// 채팅 해제 버튼
const statusChat     = $('status-chat');        // 채팅 연결 상태 표시
const chatInput      = $('chat-input');         // 채팅 텍스트 입력
const btnChatSend    = $('btn-chat-send');      // 채팅 전송 버튼

// Audio(오디오 녹음/전송) 관련 요소
const wsUrlAudioInp  = $('ws-url-audio');       // 오디오용 WS URL 입력
const codecSel       = $('codec');              // 코덱 선택
const btnAudioStart  = $('btn-audio-start');    // 오디오 녹음 시작
const btnAudioStop   = $('btn-audio-stop');     // 오디오 녹음 정지
const statusAudio    = $('status-audio');       // 오디오 상태 표시
const player         = $('player');             // 서버에서 온 오디오 재생 <audio>

// 로그 영역
const logBox         = $('log');                // 모든 채팅/로그가 표시될 DOM 요소

// ---------- Defaults ----------
// 현재 페이지가 https면 wss, 아니면 ws 사용하도록 자동 구성
wsUrlChatInp.value  = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/chat`;
wsUrlAudioInp.value = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/audio`;

// ---------- State ----------
// 현재 열린 WebSocket 핸들(채팅/오디오)
let wsChat   = null; // 채팅 WS 인스턴스
let wsAudio  = null; // 오디오 WS 인스턴스

// MediaRecorder와 스트림 제어용 상태
let stream   = null;     // getUserMedia가 돌려주는 MediaStream
let recorder = null;     // MediaRecorder 인스턴스
let lastObjectUrl = null;// 재생에 썼던 ObjectURL (누수 방지로 revoke할 때 사용)

// 서버로 보내는 오디오 메타 정보
const meta = { mimeType: '', sampleRate: 48000, channels: 1 };

// 오디오 청크 전송 시 순서 보장을 위한 시퀀스 번호(4바이트 헤더로 보냄)
let seq = 1;

// FINISH 요청 후, 서버의 "최종 1건 바이너리 응답"만 기다릴지 여부
let awaitingFinal = false;
// FINISH 대기용 Promise resolve/reject 보관 (비동기 제어)
let finalResolve = null;
let finalReject  = null;

// 기록용: 녹음 시작 시각, 전송한 청크 수
let recordStartAt = 0;
let sentChunks = 0;

// ---------- Utils ----------

/**
 * 현재 선택된 언어 값을 안전하게 얻는다.
 * @returns {string} 선택된 언어 코드 (예: 'Kor'), 없으면 'Kor'
 */
function currentLang() {
    const v = (langSel && typeof langSel.value === 'string') ? langSel.value.trim() : 'Kor';
    // 유효한 값인지 확인 후 반환
    return ['Kor','Eng','Jpn','Chn'].includes(v) ? v : 'Kor';
}

/**
 * 언어가 선택되었는지 확인.
 * 미선택 시 경고 후 focus.
 * @returns {boolean} 언어가 선택되었으면 true
 */
function ensureLangSelected() {
    const v = (langSel && typeof langSel.value === 'string') ? langSel.value.trim() : '';
    if (v === '') {
        alert('언어를 선택하세요.');
        try { langSel && langSel.focus(); } catch {} // focus 시도
        return false;
    }
    return true;
}

/**
 * [신규] 순수 텍스트를 로그 영역에 추가하는 함수.
 * 시스템 메시지나 디버깅 로그에 사용.
 * (textContent 사용, HTML 렌더링 안됨)
 * @param {...any} args - 로그에 추가할 내용들
 */
function logRaw(...args) {
    if (!logBox) return; // 로그 박스 없으면 return
    // 인자들을 문자열로 변환 (객체는 JSON)
    const line = args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ');

    const p = document.createElement('p'); // 새 <p> 태그 생성
    p.textContent = line; // 텍스트 콘텐츠로 설정
    logBox.appendChild(p); // 로그 박스에 추가
    logBox.scrollTop = logBox.scrollHeight; // 자동 스크롤
}

/**
 * [신규] HTML 문자열을 로그 영역에 렌더링하는 함수.
 * innerHTML을 사용해 링크(<a>) 등을 표시.
 * (Marked.js 변환 HTML 렌더링 시 사용)
 * @param {string} htmlString - 렌더링할 HTML 문자열
 */
function logHtml(htmlString) {
    if (!logBox) return; // 로그 박스 없으면 return
    const div = document.createElement('div'); // 새 <div> 태그 생성

    // 보안 참고: 실제 서비스에서는 DOMPurify와 같은 라이브러리로
    // 이 HTML 문자열을 정화(sanitize)하는 것이 안전하다. (XSS 방지)
    div.innerHTML = htmlString; // HTML 콘텐츠로 설정

    logBox.appendChild(div); // 로그 박스에 추가
    logBox.scrollTop = logBox.scrollHeight; // 자동 스크롤
}


/**
 * 브라우저가 지원하는 녹음 코덱(mime)을 고른다.
 * @param {string} preferred - 사용자가 'auto' 외에 선호하는 코덱
 * @returns {string} 지원되는 MIME 타입. 없으면 빈 문자열
 */
function pickSupportedMime(preferred) {
    // 1. 사용자가 선택한 코덱(auto 아님) 지원 확인
    if (preferred && preferred !== 'auto' && MediaRecorder.isTypeSupported(preferred)) return preferred;

    // 2. 'auto'거나 미지원 시, 후보 목록에서 탐색
    const candidates = ['audio/webm;codecs=opus', 'audio/ogg;codecs=opus', 'audio/webm'];
    for (const c of candidates) {
        if (MediaRecorder.isTypeSupported(c)) return c; // 가장 먼저 찾은 지원 코덱 반환
    }
    return ''; // 지원 코덱 없음
}

/**
 * AudioContext를 잠깐 열어 샘플레이트 추정.
 * @returns {Promise<number>} 샘플레이트 (Hz), 실패 시 48000
 */
async function getSampleRateViaAudioContext() {
    const AudioCtx = window.AudioContext || window.webkitAudioContext; // 브라우저 호환성
    if (!AudioCtx) return 48000; // AudioContext 미지원 시 기본값

    const ctx = new AudioCtx(); // 컨텍스트 생성
    const rate = ctx.sampleRate || 48000; // 샘플레이트 확인
    await ctx.close().catch(() => {}); // 컨텍스트 즉시 닫기
    return rate;
}

/**
 * 서버에서 받은 ArrayBuffer가 OGG/WebM인지 시그니처 바이트를 보고 추정.
 * @param {ArrayBuffer} ab - 서버에서 받은 오디오 데이터
 * @returns {string} 추정되는 MIME 타입 (기본 'audio/ogg')
 */
function guessAudioMimeFromBytes(ab) {
    const u8 = new Uint8Array(ab);
    const ogg  = [0x4f,0x67,0x67,0x53]; // 'OggS'
    const ebml = [0x1A,0x45,0xDF,0xA3]; // WebM (EBML)

    // 파일 시그니처 확인
    const startsWith = (sig) => sig.every((b,i) => u8[i] === b);

    if (u8.length >= 4 && startsWith(ogg))  return 'audio/ogg';
    if (u8.length >= 4 && startsWith(ebml)) return 'audio/webm';

    return 'audio/ogg'; // 모르면 OGG로 가정
}

/**
 * 서버 바이너리 오디오(ArrayBuffer) 재생.
 * @param {ArrayBuffer} ab - 재생할 오디오 데이터
 */
function playArrayBuffer(ab) {
    try {
        const mime = guessAudioMimeFromBytes(ab); // MIME 타입 추정
        const blob = new Blob([ab], { type: mime }); // Blob 객체 생성

        // 이전 Object URL 해제 (메모리 누수 방지)
        if (lastObjectUrl) URL.revokeObjectURL(lastObjectUrl);

        lastObjectUrl = URL.createObjectURL(blob); // 새 Object URL 생성
        player.src = lastObjectUrl; // player 소스로 설정
        player.play().catch(() => {}); // 재생 시도 (자동재생 실패 가능)
        logRaw('🔊 [audio] play server audio:', mime, blob.size, 'bytes');
    } catch (e) {
        logRaw('audio play err:', e);
    }
}

/**
 * MediaRecorder 버퍼 강제 flush (dataavailable 이벤트 발생).
 * @param {MediaRecorder} rec - MediaRecorder 인스턴스
 * @param {number} retries - 재시도 횟수
 */
async function flushRecorderOnce(rec, retries = 2) {
    for (let i = 0; i <= retries; i++) {
        const size = await new Promise((resolve) => {
            // 'dataavailable' 일회성 리스너 등록
            const handler = (e) => resolve(e?.data?.size || 0); // 데이터 크기 반환
            rec.addEventListener('dataavailable', handler, { once: true });

            try {
                rec.requestData(); // 데이터 요청
            } catch {
                resolve(0); // 에러 시 0 반환
            }
        });

        if (size > 0) return; // 데이터 얻었으면 종료

        // 데이터 없으면 잠시 대기 후 재시도
        await new Promise(r => setTimeout(r, 80));
    }
}

/**
 * MediaRecorder 'inactive' (stop) 대기.
 * @param {MediaRecorder} rec - MediaRecorder 인스턴스
 * @returns {Promise<void>}
 */
function waitRecorderStop(rec) {
    return new Promise((resolve) => {
        if (!rec || rec.state === 'inactive') return resolve(); // 이미 중지됨
        // 'stop' 일회성 리스너 등록
        rec.addEventListener('stop', () => resolve(), { once: true });
    });
}

// ---------- [추가!] Marked.js 전역 설정 ----------
// 페이지 로드 시 1회 실행, 모든 marked.parse()에 적용.

// 1. Marked.js 기본 렌더러 가져오기.
const renderer = new marked.Renderer();
const originalLinkRenderer = renderer.link; // 기존 링크 렌더러 백업

// 2. 링크(<a>) 생성 함수 재정의.
renderer.link = (href, title, text) => {
    // 기존 렌더러 호출해 기본 <a> 태그 생성.
    const html = originalLinkRenderer.call(renderer, href, title, text);

    // 생성된 HTML에 target="_blank" rel="noopener noreferrer" 추가.
    // (모든 링크 새 탭 + 보안 강화)
    return html.replace(/^<a /, '<a target="_blank" rel="noopener noreferrer" ');
};

// 3. 수정한 렌더러를 전역 옵션으로 설정.
// 이제 marked.parse()가 이 렌더러를 사용.
marked.setOptions({
    renderer: renderer
});


// ---------- Chat WS ----------
/**
 * 채팅 WS 연결 시작.
 */
function connectChat() {
    if (!ensureLangSelected()) return; // 언어 선택 확인
    if (wsChat && wsChat.readyState === WebSocket.OPEN) return; // 이미 연결됨

    const url = wsUrlChatInp.value.trim(); // URL 가져오기
    wsChat = new WebSocket(url); // WebSocket 객체 생성
    wsChat.binaryType = 'arraybuffer'; // 바이너리 수신 타입 ArrayBuffer

    // WS 연결 성공
    wsChat.onopen = () => {
        statusChat.textContent = 'connected'; // 상태 업데이트
        btnChatConn.disabled = true; // 연결 버튼 비활성화
        btnChatDisc.disabled = false; // 해제 버튼 활성화
        btnChatSend.disabled = false; // 전송 버튼 활성화
        logRaw('🔌 [chat] open:', url);
    };

    // [핵심] 서버 메시지 수신
    wsChat.onmessage = (ev) => {

        // 1. 문자열(JSON) 데이터 확인
        if (typeof ev.data === 'string') {
            try {
                // 2. JSON 파싱
                const obj = JSON.parse(ev.data);

                // 3. 'result'/'original_text' 이벤트 (LLM 답변) 확인
                if (obj && obj.data && typeof obj.data.text === 'string' &&
                    (obj.event === 'result' || obj.event === 'original_text')) {

                    const markdownText = obj.data.text; // Markdown이 포함된 텍스트

                    // 4. marked.parse()로 Markdown -> HTML 변환
                    //    (전역 설정된 렌더러 적용됨)
                    const renderedHtml = marked.parse(markdownText);

                    // 5. [디버깅] 변환된 HTML 콘솔 출력
                    console.log("Rendered HTML:", renderedHtml);

                    // 6. logHtml()로 화면에 렌더링
                    logHtml(renderedHtml);

                } else {
                    // 7. 그 외 JSON 메시지는 logRaw()로 로깅
                    logRaw('⬅️ [chat/json] ' + ev.data);
                }
            } catch (e) {
                // 8. JSON 파싱 실패 시, 순수 텍스트로 logRaw()
                logRaw('⬅️ [chat/text] ' + ev.data);
                console.error("Received non-JSON text or parsing failed:", e);
            }

            // 9. 바이너리(ArrayBuffer) 수신 (TTS 오디오)
        } else if (ev.data instanceof ArrayBuffer) {
            logRaw('⬅️ [chat/bin]', ev.data.byteLength, 'bytes');
            playArrayBuffer(ev.data); // 오디오 재생

            // 10. Blob 수신 (호환성)
        } else if (ev.data instanceof Blob) {
            // Blob -> ArrayBuffer 변환 후 재생
            ev.data.arrayBuffer().then(playArrayBuffer);
        }
    };


    // WS 연결 종료
    wsChat.onclose = (e) => {
        statusChat.textContent = 'disconnected'; // 상태 업데이트
        btnChatConn.disabled = false; // 연결 버튼 활성화
        btnChatDisc.disabled = true; // 해제 버튼 비활성화
        btnChatSend.disabled = true; // 전송 버튼 비활성화
        logRaw('🔌 [chat] close:', e.code, e.reason || '');
    };

    // WS 에러
    wsChat.onerror = (e) => logRaw('[chat][error]', e?.message || e);
}

/**
 * 채팅 WS 연결 종료.
 */
function disconnectChat() {
    try { wsChat && wsChat.close(1000, 'client-close'); } catch {}
}

/**
 * 채팅 텍스트 전송.
 */
function sendChat() {
    if (!wsChat || wsChat.readyState !== WebSocket.OPEN) return alert('채팅 WS에 먼저 연결하세요.');
    if (!ensureLangSelected()) return; // 언어 선택 확인

    const text = chatInput.value.trim(); // 입력 텍스트
    if (!text) return; // 빈 메시지 전송 방지

    // JSON 포맷으로 전송
    wsChat.send(JSON.stringify({ type: 'CHAT', text, lang: currentLang() }));

    logRaw('➡️ [chat/send]', text); // 보낸 메시지 로깅
    chatInput.value = ''; // 입력창 비우기
}

// ---------- Audio WS + Recording ----------
// (오디오 코드는 log() -> logRaw() 변경 외 큰 변화 없음)

/**
 * 오디오 녹음/스트리밍 시작.
 */
async function startAudio() {
    if (!ensureLangSelected()) return; // 언어 선택 확인

    try {
        // UI 버튼 상태 변경
        btnAudioStart.disabled = true;
        btnAudioStop.disabled  = true;
        statusAudio.textContent = 'preparing…';

        // 1. 지원되는 코덱 선택
        const chosen = pickSupportedMime(codecSel.value);
        if (!chosen) {
            logRaw('❌ [audio] 지원 가능한 녹음 코덱을 찾지 못했습니다.');
            statusAudio.textContent = 'error';
            btnAudioStart.disabled = false;
            return;
        }

        // 2. 마이크 권한 요청/스트림 가져오기
        stream = await navigator.mediaDevices.getUserMedia({
            audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true, autoGainControl: true }
        });

        // 3. 오디오 메타 정보 설정
        const track = stream.getAudioTracks()[0];
        const settings = track.getSettings ? track.getSettings() : {};
        meta.sampleRate = settings.sampleRate || await getSampleRateViaAudioContext(); // 실제 샘플레이트
        meta.channels   = settings.channelCount || 1;
        meta.mimeType   = chosen;

        // 4. 오디오 WS 연결
        const url = wsUrlAudioInp.value.trim();
        wsAudio = new WebSocket(url);
        wsAudio.binaryType = 'arraybuffer';

        // 오디오 WS 연결 성공
        wsAudio.onopen = () => {
            statusAudio.textContent = 'recording';
            logRaw('🔌 [audio] open:', url);

            // 5. 'START' 메시지 전송 (메타정보, 핸드셰이크)
            wsAudio.send(JSON.stringify({
                type: 'START',
                lang: currentLang(),
                mimeType: meta.mimeType,
                sampleRate: meta.sampleRate,
                channels: meta.channels
            }));

            // 6. MediaRecorder 설정
            seq = 1; // 시퀀스 번호 초기화
            sentChunks = 0; // 전송 청크 수 초기화
            recorder = new MediaRecorder(stream, { mimeType: meta.mimeType });

            // 녹음 시작 시
            recorder.onstart = () => {
                recordStartAt = performance.now(); // 시작 시간 기록
                logRaw('⏺️ [audio] recording started:', meta.mimeType, meta.sampleRate + 'Hz');
                // 빠른 정지 버튼 클릭 방지
                setTimeout(() => { btnAudioStop.disabled = false; }, 600);
            };

            // [핵심] 녹음 데이터(청크) 발생 시 (ondataavailable)
            recorder.ondataavailable = async (e) => {
                if (!wsAudio || wsAudio.readyState !== WebSocket.OPEN) return; // WS 닫혔으면 무시
                if (!e.data || e.data.size === 0) {
                    try { recorder.requestData(); } catch {} // 데이터 없으면 다음 데이터 요청
                    return;
                }

                // 7. 데이터 전송 (프로토콜: 4B seq + data)
                const body = await e.data.arrayBuffer(); // Blob을 ArrayBuffer로
                const out  = new Uint8Array(4 + body.byteLength); // 헤더(4b) + 바디
                const view = new DataView(out.buffer);
                view.setUint32(0, seq); // 4B 헤더에 seq 쓰기
                out.set(new Uint8Array(body), 4); // 4B 뒤에 오디오 데이터 쓰기

                wsAudio.send(out); // WS 전송

                sentChunks++;
                logRaw('➡️ [audio/chunk] seq=', seq, 'bytes=', body.byteLength, 'sentChunks=', sentChunks);
                seq++; // 시퀀스 번호 증가
            };

            recorder.onstop  = () => logRaw('⏹️ [audio] recording stopped');
            recorder.onerror = (ev) => logRaw('[audio][recorder][error]', ev?.error || ev);

            // 8. 녹음 시작 (100ms 간격)
            recorder.start(100);
        };

        // 오디오 WS 메시지 수신
        wsAudio.onmessage = (ev) => {
            // 바이너리 처리 함수 (TTS 응답)
            const handleBuf = (buf) => {
                const u8 = new Uint8Array(buf);
                if (u8.length >= 4) {
                    // 서버 응답 헤더(seq) 가정
                    const v = new DataView(u8.buffer, u8.byteOffset, u8.byteLength);
                    const rseq = v.getUint32(0); // 응답 시퀀스
                    const payload = u8.slice(4).buffer; // 실제 오디오 데이터
                    logRaw('⬅️ [audio/bin] seq=', rseq, 'bytes=', u8.length - 4);

                    // 'FINISH' 응답 대기 중이었다면
                    if (awaitingFinal && finalResolve) {
                        awaitingFinal = false; // 대기 상태 해제
                        try { playArrayBuffer(payload); } catch {} // 마지막 오디오 재생
                        finalResolve(); // stopAudio()의 Promise를 resolve
                        return;
                    }
                    // 일반 응답 오디오 재생
                    playArrayBuffer(payload);
                } else {
                    logRaw('⬅️ [audio/bin] recv(no header?) bytes=', u8.length);
                }
            };

            if (typeof ev.data === 'string') {
                // 텍스트(JSON) 수신 (STT 중간 결과 등)
                try {
                    const obj = JSON.parse(ev.data);
                    logRaw('⬅️ [audio/text]', obj.type ? `${obj.type}:` : '', obj.text ?? ev.data);
                } catch {
                    logRaw('⬅️ [audio/text]', ev.data); // JSON 파싱 실패
                }
            } else if (ev.data instanceof ArrayBuffer) {
                handleBuf(ev.data); // 바이너리(ArrayBuffer) 처리
            } else if (ev.data instanceof Blob) {
                ev.data.arrayBuffer().then(handleBuf); // 바이너리(Blob) 처리
            }
        };

        // 오디오 WS 종료
        wsAudio.onclose = (e) => {
            logRaw('🔌 [audio] close:', e.code, e.reason || '');
            cleanupAudio(); // 리소스 정리
        };
        // 오디오 WS 에러
        wsAudio.onerror = (e) => {
            logRaw('[audio][ws][error]', e?.message || e);
            cleanupAudio(); // 리소스 정리
        };
    } catch (e) {
        // startAudio() 자체 에러 (마이크 권한 거부 등)
        logRaw('❌ [audio] start failed:', e?.message || e);
        statusAudio.textContent = 'error';
        cleanupAudio(); // 리소스 정리
    }
}

/**
 * 서버에 'FINISH' 전송, 최종 응답 대기.
 * @param {number} timeoutMs - 타임아웃 시간 (ms)
 * @returns {Promise<void>}
 */
function requestFinalMerge(timeoutMs = 12000) {
    if (!wsAudio || wsAudio.readyState !== WebSocket.OPEN) {
        return Promise.reject(new Error('audio ws not open'));
    }

    awaitingFinal = true; // 최종 응답 대기 상태
    wsAudio.send(JSON.stringify({ type: 'FINISH' })); // 'FINISH' 메시지 전송

    // 이 Promise는 onmessage에서 resolve되거나 timeout됨.
    return new Promise((resolve, reject) => {
        finalResolve = resolve;
        finalReject  = reject;

        // 타임아웃 타이머
        setTimeout(() => {
            if (awaitingFinal) { // 아직도 대기 중이면
                awaitingFinal = false; // 대기 상태 해제
                reject(new Error('final merge timeout')); // 타임아웃 에러
            }
        }, timeoutMs);
    });
}

/**
 * 오디오 녹음 중지, 서버에 'FINISH' 전송.
 */
async function stopAudio() {
    // 0.6초 미만 녹음 시 잠시 대기 (서버 부하 방지)
    const elapsed = performance.now() - recordStartAt;
    if (elapsed < 600) await new Promise(r => setTimeout(r, 600 - elapsed));

    // 1. MediaRecorder 중지
    if (recorder && recorder.state !== 'inactive') {
        await flushRecorderOnce(recorder).catch(() => {}); // 남은 버퍼 비우기
        const pStop = waitRecorderStop(recorder); // stop 이벤트 대기 Promise
        try { recorder.stop(); } catch {} // 중지 명령
        await pStop; // 실제로 중지될 때까지 대기
        await new Promise(r => setTimeout(r, 200)); // 마지막 청크가 전송될 시간 확보
    }

    let mergeOk = false;

    // 2. 'FINISH' 전송 및 최종 응답 대기
    if (wsAudio && wsAudio.readyState === WebSocket.OPEN) {
        try {
            statusAudio.textContent = 'finalizing…'; // 상태 업데이트
            await requestFinalMerge(12000); // 최종 응답 기다리기
            mergeOk = true; // 성공
        } catch (e) {
            logRaw('⚠️ [audio] final merge error:', e?.message || e);
        }
    }

    // 3. WS 연결 종료
    if (wsAudio && wsAudio.readyState === WebSocket.OPEN) {
        try { wsAudio.close(1000, mergeOk ? 'final-sent' : 'client-stop'); } catch {}
    }

    // 4. 리소스 정리
    cleanupAudio();
}

/**
 * 오디오 관련 모든 상태/리소스/UI 초기화.
 */
function cleanupAudio() {
    // UI 초기화
    btnAudioStart.disabled = false;
    btnAudioStop.disabled  = true;
    statusAudio.textContent = 'idle';

    // MediaRecorder 정리
    if (recorder && recorder.state !== 'inactive') {
        try { recorder.stop(); } catch {}
    }
    recorder = null;

    // 마이크 스트림(MediaStream) 정리
    if (stream) {
        try { stream.getTracks().forEach(t => t.stop()); } catch {}
    }
    stream = null;

    // WebSocket 정리
    if (wsAudio) {
        try { wsAudio.close(); } catch {}
    }
    wsAudio = null;

    // 비동기 상태 초기화
    awaitingFinal = false;
    finalResolve = null;
    finalReject  = null;
}

// ---------- Events ----------
// 버튼/입력 이벤트 바인딩

// 채팅 연결/해제/전송 버튼
btnChatConn.addEventListener('click', connectChat);
btnChatDisc.addEventListener('click', disconnectChat);
btnChatSend.addEventListener('click', sendChat);
// 채팅 입력창 Enter키 전송
chatInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') sendChat(); });

// 오디오 시작/정지 버튼
btnAudioStart.addEventListener('click', startAudio);
btnAudioStop.addEventListener('click', stopAudio);

// 페이지 떠나기 전 리소스 정리 (새로고침, 닫기 등)
window.addEventListener('beforeunload', () => {
    try { disconnectChat(); } catch {} // 채팅 WS 닫기
    try { stopAudio(); } catch {}      // 오디오 정리 (WS 닫기 포함)
    if (lastObjectUrl) URL.revokeObjectURL(lastObjectUrl); // 오디오 URL 해제
});