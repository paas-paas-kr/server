// ---------- DOM ----------
// 간단한 헬퍼: id로 DOM 요소 가져오기
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
const logBox         = $('log');

// ---------- Defaults ----------
// 현재 페이지가 https면 wss, 아니면 ws 사용하도록 자동 구성
wsUrlChatInp.value  = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/chat`;
wsUrlAudioInp.value = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/audio`;

// ---------- State ----------
// 현재 열린 WebSocket 핸들(채팅/오디오)
let wsChat   = null;
let wsAudio  = null;

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
// FINISH 대기용 Promise resolve/reject 보관
let finalResolve = null;
let finalReject  = null;

// 기록용: 녹음 시작 시각, 전송한 청크 수
let recordStartAt = 0;
let sentChunks = 0;

// ---------- Utils ----------

// 현재 선택된 언어 값을 안전하게 얻기
function currentLang() {
    const v = (langSel && typeof langSel.value === 'string') ? langSel.value.trim() : 'Kor';
    return ['Kor','Eng','Jpn','Chn'].includes(v) ? v : 'Kor';
}

// 언어가 선택되었는지 확인 (미선택 시 경고 후 focus)
function ensureLangSelected() {
    const v = (langSel && typeof langSel.value === 'string') ? langSel.value.trim() : '';
    if (v === '') {
        alert('언어를 선택하세요.');
        try { langSel && langSel.focus(); } catch {}
        return false;
    }
    return true;
}

// 화면 로그 유틸: 문자열/객체를 한 줄로 이어붙여 logBox에 출력
function log(...args) {
    const line = args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ');
    logBox.textContent += line + '\n';
    logBox.scrollTop = logBox.scrollHeight;
}

// 브라우저가 지원하는 녹음 코덱(mime)을 고른다.
// - 사용자가 지정한 preferred가 유효하면 우선
// - 아니면 후보 리스트에서 가능한 첫 번째로
function pickSupportedMime(preferred) {
    if (preferred && preferred !== 'auto' && MediaRecorder.isTypeSupported(preferred)) return preferred;
    const candidates = ['audio/webm;codecs=opus', 'audio/ogg;codecs=opus', 'audio/webm'];
    for (const c of candidates) if (MediaRecorder.isTypeSupported(c)) return c;
    return ''; // 전혀 지원 안 되면 빈 문자열
}

// AudioContext를 잠깐 열어 샘플레이트 추정(마이크 설정 정보에 없는 브라우저 대비)
async function getSampleRateViaAudioContext() {
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return 48000;
    const ctx = new AudioCtx();
    const rate = ctx.sampleRate || 48000;
    await ctx.close().catch(() => {});
    return rate;
}

// 서버에서 받은 ArrayBuffer가 OGG/WebM인지 간단 시그니처로 추정(재생 MIME용)
function guessAudioMimeFromBytes(ab) {
    const u8 = new Uint8Array(ab);
    const ogg  = [0x4f,0x67,0x67,0x53];     // "OggS"
    const ebml = [0x1A,0x45,0xDF,0xA3];     // EBML 헤더(webm)
    const startsWith = (sig) => sig.every((b,i) => u8[i] === b);
    if (u8.length >= 4 && startsWith(ogg))  return 'audio/ogg';
    if (u8.length >= 4 && startsWith(ebml)) return 'audio/webm';
    // 모호할 땐 ogg로 기본 처리
    return 'audio/ogg';
}

// 서버 바이너리 오디오(ArrayBuffer)를 <audio>로 재생
function playArrayBuffer(ab) {
    try {
        const mime = guessAudioMimeFromBytes(ab);
        const blob = new Blob([ab], { type: mime });
        if (lastObjectUrl) URL.revokeObjectURL(lastObjectUrl); // 이전 URL 해제
        lastObjectUrl = URL.createObjectURL(blob);
        player.src = lastObjectUrl;
        player.play().catch(() => {}); // 자동재생 차단 시 무시
        log('🔊 [audio] play server audio:', mime, blob.size, 'bytes');
    } catch (e) {
        log('audio play err:', e);
    }
}

// MediaRecorder가 내부 버퍼를 가지고 있을 수 있어, stop 전에 강제로 한번 비우기
// - requestData() 호출하면 dataavailable 이벤트가 1회 더 발생
// - 빈 청크면 짧게 재시도
async function flushRecorderOnce(rec, retries = 2) {
    for (let i = 0; i <= retries; i++) {
        const size = await new Promise((resolve) => {
            const handler = (e) => resolve(e?.data?.size || 0);
            rec.addEventListener('dataavailable', handler, { once: true });
            try { rec.requestData(); } catch { resolve(0); }
        });
        if (size > 0) return;                // 뭔가 나온 경우 성공
        await new Promise(r => setTimeout(r, 80)); // 잠깐 대기 후 재시도
    }
}

// MediaRecorder가 완전히 'inactive' 상태가 될 때까지 기다리기
function waitRecorderStop(rec) {
    return new Promise((resolve) => {
        if (!rec || rec.state === 'inactive') return resolve();
        rec.addEventListener('stop', () => resolve(), { once: true });
    });
}

// ---------- Chat WS ----------
// 채팅 WS 연결(텍스트 메시지 중심, 바이너리도 수신 가능)
function connectChat() {
    if (!ensureLangSelected()) return;
    if (wsChat && wsChat.readyState === WebSocket.OPEN) return; // 이미 연결돼 있으면 무시
    const url = wsUrlChatInp.value.trim();
    wsChat = new WebSocket(url);
    wsChat.binaryType = 'arraybuffer'; // 바이너리 수신 시 ArrayBuffer로 받기

    wsChat.onopen = () => {
        statusChat.textContent = 'connected';
        btnChatConn.disabled = true;
        btnChatDisc.disabled = false;
        btnChatSend.disabled = false;
        log('🔌 [chat] open:', url);
    };

    // 서버가 보내는 메시지 수신
    wsChat.onmessage = (ev) => {
        if (typeof ev.data === 'string') {
            // 텍스트면 JSON 파싱 시도 → type/text를 로그
            try {
                const obj = JSON.parse(ev.data);
                log('⬅️ [chat/text]', obj.type ? `${obj.type}:` : '', obj.text ?? ev.data);
            } catch { log('⬅️ [chat/text]', ev.data); }
        } else if (ev.data instanceof ArrayBuffer) {
            // 바이너리면 크기만 로그 + 재생 시도(필요할 때만)
            log('⬅️ [chat/bin]', ev.data.byteLength, 'bytes');
            playArrayBuffer(ev.data);
        } else if (ev.data instanceof Blob) {
            // Blob으로 오면 ArrayBuffer로 바꿔 처리
            ev.data.arrayBuffer().then(playArrayBuffer);
        }
    };

    wsChat.onclose = (e) => {
        statusChat.textContent = 'disconnected';
        btnChatConn.disabled = false;
        btnChatDisc.disabled = true;
        btnChatSend.disabled = true;
        log('🔌 [chat] close:', e.code, e.reason || '');
    };

    wsChat.onerror = (e) => log('[chat][error]', e?.message || e);
}

// 채팅 WS 종료
function disconnectChat() {
    try { wsChat && wsChat.close(1000, 'client-close'); } catch {}
}

// 채팅 텍스트 전송
function sendChat() {
    if (!wsChat || wsChat.readyState !== WebSocket.OPEN) return alert('채팅 WS에 먼저 연결하세요.');
    if (!ensureLangSelected()) return;

    const text = chatInput.value.trim();
    if (!text) return;
    // 서버 프로토콜: { type:'CHAT', text, lang }
    wsChat.send(JSON.stringify({ type: 'CHAT', text, lang: currentLang() }));
    log('➡️ [chat/send]', text);
    chatInput.value = '';
}

// ---------- Audio WS + Recording ----------
// 오디오 녹음 시작 + WS 연결 + START 메타 전송 + 청크 스트리밍
async function startAudio() {
    if (!ensureLangSelected()) return;

    try {
        // UI 잠금 및 상태 표기
        btnAudioStart.disabled = true;
        btnAudioStop.disabled  = true;
        statusAudio.textContent = 'preparing…';

        // 브라우저가 지원하는 mime 고르기(선호 코덱 우선)
        const chosen = pickSupportedMime(codecSel.value);
        if (!chosen) {
            log('❌ [audio] 지원 가능한 녹음 코덱을 찾지 못했습니다.');
            statusAudio.textContent = 'error';
            btnAudioStart.disabled = false;
            return;
        }

        // 마이크 권한 및 스트림 얻기
        stream = await navigator.mediaDevices.getUserMedia({
            audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true, autoGainControl: true }
        });

        // 실제 마이크 설정에서 샘플레이트/채널 조회(없으면 AudioContext로 추정)
        const track = stream.getAudioTracks()[0];
        const settings = track.getSettings ? track.getSettings() : {};
        meta.sampleRate = settings.sampleRate || await getSampleRateViaAudioContext();
        meta.channels   = settings.channelCount || 1;
        meta.mimeType   = chosen;

        // 오디오용 WebSocket 연결
        const url = wsUrlAudioInp.value.trim();
        wsAudio = new WebSocket(url);
        wsAudio.binaryType = 'arraybuffer';

        wsAudio.onopen = () => {
            statusAudio.textContent = 'recording';
            log('🔌 [audio] open:', url);

            // 1) START 메타(TEXT 프레임) 전송: 서버는 이 정보를 보고 녹음 포맷을 이해/검증
            // { "type":"START", "lang": "...", "mimeType": "...", "sampleRate": 48000, "channels": 1 }
            wsAudio.send(JSON.stringify({
                type: 'START',
                lang: currentLang(),
                mimeType: meta.mimeType,
                sampleRate: meta.sampleRate,
                channels: meta.channels
            }));

            // 시퀀스 초기화
            seq = 1;
            sentChunks = 0;

            // 2) MediaRecorder 생성(선택한 mime로 녹음)
            recorder = new MediaRecorder(stream, { mimeType: meta.mimeType });

            // 녹음 시작 이벤트
            recorder.onstart = () => {
                recordStartAt = performance.now();
                log('⏺️ [audio] recording started:', meta.mimeType, meta.sampleRate + 'Hz');
                // 최소 녹음시간(600ms) 보장 전에는 stop 버튼 잠시 비활성화 → 빈 청크 방지
                setTimeout(() => { btnAudioStop.disabled = false; }, 600);
            };

            // 핵심: 청크가 도착할 때마다 바이너리 프레임으로 전송
            recorder.ondataavailable = async (e) => {
                // 소켓이 열려있을 때만 처리
                if (!wsAudio || wsAudio.readyState !== WebSocket.OPEN) return;
                // 비어 있으면 한번 더 requestData()
                if (!e.data || e.data.size === 0) { try { recorder.requestData(); } catch {} return; }

                // Blob → ArrayBuffer
                const body = await e.data.arrayBuffer();

                // [ 4바이트(seq, big-endian) | payload(body) ] 포맷의 바이트 배열 생성
                const out  = new Uint8Array(4 + body.byteLength);
                const view = new DataView(out.buffer);
                view.setUint32(0, seq);               // 서버의 toIntBE(0)과 호환되는 big-endian
                out.set(new Uint8Array(body), 4);     // 뒤에 오디오 바이트 부착

                // 3) 바이너리 프레임 전송: 서버에선 msg.getType()==BINARY로 수신
                wsAudio.send(out);
                sentChunks++;
                log('➡️ [audio/chunk] seq=', seq, 'bytes=', body.byteLength, 'sentChunks=', sentChunks);
                seq++;
            };

            recorder.onstop  = () => log('⏹️ [audio] recording stopped');
            recorder.onerror = (ev) => log('[audio][recorder][error]', ev?.error || ev);

            // timeslice=100ms → 100ms마다 dataavailable 이벤트 발생 (레이턴시 개선)
            recorder.start(100);
        };

        // 서버가 보내는 메시지(텍스트/바이너리) 처리
        wsAudio.onmessage = (ev) => {
            // 내부 헬퍼: 서버 바이너리 응답 처리
            const handleBuf = (buf) => {
                const u8 = new Uint8Array(buf);
                if (u8.length >= 4) {
                    // (옵션) 서버도 4바이트 헤더(seq)를 붙여줄 수 있다고 가정
                    const v = new DataView(u8.buffer, u8.byteOffset, u8.byteLength);
                    const rseq = v.getUint32(0);
                    const payload = u8.slice(4).buffer;
                    log('⬅️ [audio/bin] seq=', rseq, 'bytes=', u8.length - 4);

                    // FINISH 이후 단 한 건만 기다리는 모드일 때
                    if (awaitingFinal && finalResolve) {
                        awaitingFinal = false;
                        try { playArrayBuffer(payload); } catch {}
                        finalResolve();
                        return;
                    }
                    // 실시간 스트리밍 TTS 같은 경우 바로 재생
                    playArrayBuffer(payload);
                } else {
                    log('⬅️ [audio/bin] recv(no header?) bytes=', u8.length);
                }
            };

            if (typeof ev.data === 'string') {
                // 서버에서 오는 텍스트(로그/오류/시스템 메시지)
                try {
                    const obj = JSON.parse(ev.data);
                    log('⬅️ [audio/text]', obj.type ? `${obj.type}:` : '', obj.text ?? ev.data);
                } catch { log('⬅️ [audio/text]', ev.data); }
            } else if (ev.data instanceof ArrayBuffer) {
                handleBuf(ev.data);
            } else if (ev.data instanceof Blob) {
                // Blob은 ArrayBuffer로 변환 후 동일하게 처리
                ev.data.arrayBuffer().then(handleBuf);
            }
        };

        // 연결 종료/오류 시 정리
        wsAudio.onclose = (e) => {
            log('🔌 [audio] close:', e.code, e.reason || '');
            cleanupAudio();
        };

        wsAudio.onerror = (e) => {
            log('[audio][ws][error]', e?.message || e);
            cleanupAudio();
        };

    } catch (e) {
        // 준비/권한/장치 문제 등으로 실패 시 상태 복구
        log('❌ [audio] start failed:', e?.message || e);
        statusAudio.textContent = 'error';
        cleanupAudio();
    }
}

// 서버에 “최종 병합 요청”(TEXT: FINISH)을 보내고,
// 서버의 “최종 1건 바이너리 응답”을 timeout 내에 기다린다.
function requestFinalMerge(timeoutMs = 12000) {
    if (!wsAudio || wsAudio.readyState !== WebSocket.OPEN) {
        return Promise.reject(new Error('audio ws not open'));
    }
    awaitingFinal = true;
    // FINISH 신호(TEXT 프레임) 전송 → 서버는 aggregator.merge() 후 processFinal(...)
    wsAudio.send(JSON.stringify({ type: 'FINISH' }));

    return new Promise((resolve, reject) => {
        finalResolve = resolve;
        finalReject  = reject;
        // timeout 내 응답이 없으면 에러
        setTimeout(() => {
            if (awaitingFinal) {
                awaitingFinal = false;
                reject(new Error('final merge timeout'));
            }
        }, timeoutMs);
    });
}

// 오디오 녹음을 멈추고 FINISH 수행 → 서버의 최종 응답을 받고 종료
async function stopAudio() {
    // 너무 짧은 녹음 방지: 최소 600ms 보장
    const elapsed = performance.now() - recordStartAt;
    if (elapsed < 600) await new Promise(r => setTimeout(r, 600 - elapsed));

    // Recorder 내부 버퍼 비우기 → stop → 완전 정지 대기
    if (recorder && recorder.state !== 'inactive') {
        await flushRecorderOnce(recorder).catch(() => {});
        const pStop = waitRecorderStop(recorder);
        try { recorder.stop(); } catch {}
        await pStop;
        // onstop 이후에도 늦게 오는 dataavailable 수용을 위해 200ms 여유
        await new Promise(r => setTimeout(r, 200));
    }

    // 서버 최종 병합 요청 및 응답 수신 시도
    let mergeOk = false;
    if (wsAudio && wsAudio.readyState === WebSocket.OPEN) {
        try {
            statusAudio.textContent = 'finalizing…';
            await requestFinalMerge(12000); // FINISH 텍스트 → 최종 바이너리 1건 기다림
            mergeOk = true;
        } catch (e) {
            log('⚠️ [audio] final merge error:', e?.message || e);
        }
    }

    // 서버 응답 여부와 무관하게 소켓을 닫고 정리
    if (wsAudio && wsAudio.readyState === WebSocket.OPEN) {
        try { wsAudio.close(1000, mergeOk ? 'final-sent' : 'client-stop'); } catch {}
    }

    cleanupAudio();
}

// 오디오 관련 리소스 정리(버튼/스트림/소켓/상태 변수)
function cleanupAudio() {
    btnAudioStart.disabled = false;
    btnAudioStop.disabled  = true;
    statusAudio.textContent = 'idle';

    if (recorder && recorder.state !== 'inactive') {
        try { recorder.stop(); } catch {}
    }
    recorder = null;

    if (stream) {
        try { stream.getTracks().forEach(t => t.stop()); } catch {}
    }
    stream = null;

    if (wsAudio) {
        try { wsAudio.close(); } catch {}
    }
    wsAudio = null;

    awaitingFinal = false;
    finalResolve = null;
    finalReject  = null;
}

// ---------- Events ----------
// 버튼/입력 이벤트 바인딩
btnChatConn.addEventListener('click', connectChat);
btnChatDisc.addEventListener('click', disconnectChat);
btnChatSend.addEventListener('click', sendChat);
chatInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') sendChat(); });

btnAudioStart.addEventListener('click', startAudio);
btnAudioStop.addEventListener('click', stopAudio);

// 페이지를 떠나기 전에 열린 리소스 정리
window.addEventListener('beforeunload', () => {
    try { disconnectChat(); } catch {}
    try { stopAudio(); } catch {}
    if (lastObjectUrl) URL.revokeObjectURL(lastObjectUrl);
});
