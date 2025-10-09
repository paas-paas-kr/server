// ---------- DOM ----------
const $ = (id) => document.getElementById(id);

// Chat
const wsUrlChatInp   = $('ws-url-chat');
const btnChatConn    = $('btn-chat-connect');
const btnChatDisc    = $('btn-chat-disconnect');
const statusChat     = $('status-chat');
const chatInput      = $('chat-input');
const btnChatSend    = $('btn-chat-send');

// Audio
const wsUrlAudioInp  = $('ws-url-audio');
const codecSel       = $('codec');
const btnAudioStart  = $('btn-audio-start');
const btnAudioStop   = $('btn-audio-stop');
const statusAudio    = $('status-audio');
const player         = $('player');

// Log
const logBox         = $('log');

// ---------- Defaults ----------
wsUrlChatInp.value  = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/chat`;
wsUrlAudioInp.value = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/audio`;

// ---------- State ----------
let wsChat   = null;
let wsAudio  = null;

let stream   = null;
let recorder = null;
let lastObjectUrl = null;

const meta = { mimeType: '', sampleRate: 48000, channels: 1 };

// ✅ 시퀀스 번호
let seq = 1;

// ✅ 최종 병합 대기 상태
let awaitingFinal = false;
let finalResolve = null;
let finalReject  = null;

// ---------- Utils ----------
function log(...args) {
    const line = args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ');
    logBox.textContent += line + '\n';
    logBox.scrollTop = logBox.scrollHeight;
}

function pickSupportedMime(preferred) {
    if (preferred && preferred !== 'auto' && MediaRecorder.isTypeSupported(preferred)) return preferred;
    const candidates = ['audio/webm;codecs=opus', 'audio/ogg;codecs=opus', 'audio/webm'];
    for (const c of candidates) if (MediaRecorder.isTypeSupported(c)) return c;
    return '';
}

async function getSampleRateViaAudioContext() {
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return 48000;
    const ctx = new AudioCtx();
    const rate = ctx.sampleRate || 48000;
    await ctx.close().catch(() => {});
    return rate;
}

function guessAudioMimeFromBytes(ab) {
    const u8 = new Uint8Array(ab);
    const ogg = [0x4f,0x67,0x67,0x53];     // "OggS"
    const ebml = [0x1A,0x45,0xDF,0xA3];    // EBML (webm)
    const startsWith = (sig) => sig.every((b,i) => u8[i] === b);
    if (u8.length >= 4 && startsWith(ogg))  return 'audio/ogg';
    if (u8.length >= 4 && startsWith(ebml)) return 'audio/webm';
    return 'audio/ogg';
}

function playArrayBuffer(ab) {
    try {
        const mime = guessAudioMimeFromBytes(ab);
        const blob = new Blob([ab], { type: mime });
        if (lastObjectUrl) URL.revokeObjectURL(lastObjectUrl);
        lastObjectUrl = URL.createObjectURL(blob);
        player.src = lastObjectUrl;
        player.play().catch(() => {});
        log('🔊 [audio] play server audio:', mime, blob.size, 'bytes');
    } catch (e) {
        log('audio play err:', e);
    }
}

// ---------- Chat WS ----------
function connectChat() {
    if (wsChat && wsChat.readyState === WebSocket.OPEN) return;
    const url = wsUrlChatInp.value.trim();
    wsChat = new WebSocket(url);
    wsChat.binaryType = 'arraybuffer';

    wsChat.onopen = () => {
        statusChat.textContent = 'connected';
        btnChatConn.disabled = true;
        btnChatDisc.disabled = false;
        btnChatSend.disabled = false;
        log('🔌 [chat] open:', url);
    };

    wsChat.onmessage = (ev) => {
        if (typeof ev.data === 'string') {
            try {
                const obj = JSON.parse(ev.data);
                log('⬅️ [chat/text]', obj.type ? `${obj.type}:` : '', obj.text ?? ev.data);
            } catch { log('⬅️ [chat/text]', ev.data); }
        } else if (ev.data instanceof ArrayBuffer) {
            log('⬅️ [chat/bin]', ev.data.byteLength, 'bytes');
            playArrayBuffer(ev.data);
        } else if (ev.data instanceof Blob) {
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

function disconnectChat() {
    try { wsChat && wsChat.close(1000, 'client-close'); } catch {}
}

function sendChat() {
    if (!wsChat || wsChat.readyState !== WebSocket.OPEN) return alert('채팅 WS에 먼저 연결하세요.');
    const text = chatInput.value.trim();
    if (!text) return;
    wsChat.send(JSON.stringify({ type: 'CHAT', text }));
    log('➡️ [chat/send]', text);
    chatInput.value = '';
}

// ---------- Audio WS + Recording ----------
async function startAudio() {
    try {
        btnAudioStart.disabled = true;
        statusAudio.textContent = 'preparing…';

        const chosen = pickSupportedMime(codecSel.value);
        if (!chosen) {
            log('❌ [audio] 지원 가능한 녹음 코덱을 찾지 못했습니다.');
            statusAudio.textContent = 'error';
            btnAudioStart.disabled = false;
            return;
        }

        stream = await navigator.mediaDevices.getUserMedia({
            audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true, autoGainControl: true }
        });

        const track = stream.getAudioTracks()[0];
        const settings = track.getSettings ? track.getSettings() : {};
        meta.sampleRate = settings.sampleRate || await getSampleRateViaAudioContext();
        meta.channels   = settings.channelCount || 1;
        meta.mimeType   = chosen;

        const url = wsUrlAudioInp.value.trim();
        wsAudio = new WebSocket(url);
        wsAudio.binaryType = 'arraybuffer';

        wsAudio.onopen = () => {
            statusAudio.textContent = 'recording';
            btnAudioStop.disabled = false;
            log('🔌 [audio] open:', url);

            // 메타 전송
            wsAudio.send(JSON.stringify({
                mimeType: meta.mimeType,
                sampleRate: meta.sampleRate,
                channels: meta.channels
            }));

            // 시퀀스 초기화
            seq = 1;

            recorder = new MediaRecorder(stream, { mimeType: meta.mimeType });

            // ✅ 청크 앞에 4바이트 시퀀스 번호를 붙여 보냄
            recorder.ondataavailable = async (e) => {
                if (!e.data || e.data.size === 0) return;
                if (!wsAudio || wsAudio.readyState !== WebSocket.OPEN) return;

                const body = await e.data.arrayBuffer();
                const out  = new Uint8Array(4 + body.byteLength);
                const view = new DataView(out.buffer);
                view.setUint32(0, seq);                 // big-endian
                out.set(new Uint8Array(body), 4);

                wsAudio.send(out);
                log('➡️ [audio/chunk] seq=', seq, 'bytes=', body.byteLength);
                seq++;
            };

            recorder.onstart = () => log('⏺️ [audio] recording started:', meta.mimeType, meta.sampleRate + 'Hz');
            recorder.onstop  = () => log('⏹️ [audio] recording stopped');
            recorder.onerror = (ev) => log('[audio][recorder][error]', ev?.error || ev);

            // 250ms 단위로 청크화
            recorder.start(250);
        };

        // ✅ 최종 병합 1건 수신 처리 + 레거시 스트리밍도 안전 처리
        wsAudio.onmessage = (ev) => {
            const handleBuf = (buf) => {
                const u8 = new Uint8Array(buf);
                if (u8.length >= 4) {
                    const v = new DataView(u8.buffer, u8.byteOffset, u8.byteLength);
                    const rseq = v.getUint32(0);
                    const payload = u8.slice(4).buffer;
                    log('⬅️ [audio/bin] seq=', rseq, 'bytes=', u8.length - 4);

                    if (awaitingFinal && finalResolve) {
                        awaitingFinal = false;
                        try { playArrayBuffer(payload); } catch {}
                        finalResolve();
                        return;
                    }
                    // (스트리밍 모드 잔존 대응)
                    playArrayBuffer(payload);
                } else {
                    log('⬅️ [audio/bin] recv(no header?) bytes=', u8.length);
                }
            };

            if (typeof ev.data === 'string') {
                try {
                    const obj = JSON.parse(ev.data);
                    log('⬅️ [audio/text]', obj.type ? `${obj.type}:` : '', obj.text ?? ev.data);
                } catch { log('⬅️ [audio/text]', ev.data); }
            } else if (ev.data instanceof ArrayBuffer) {
                handleBuf(ev.data);
            } else if (ev.data instanceof Blob) {
                ev.data.arrayBuffer().then(handleBuf);
            }
        };

        wsAudio.onclose = (e) => {
            log('🔌 [audio] close:', e.code, e.reason || '');
            cleanupAudio();
        };

        wsAudio.onerror = (e) => {
            log('[audio][ws][error]', e?.message || e);
            cleanupAudio();
        };

    } catch (e) {
        log('❌ [audio] start failed:', e?.message || e);
        statusAudio.textContent = 'error';
        cleanupAudio();
    }
}

// ✅ 서버에 “최종 병합” 요청 후, 단 한 건의 바이너리를 기다림
function requestFinalMerge(timeoutMs = 8000) {
    if (!wsAudio || wsAudio.readyState !== WebSocket.OPEN) {
        return Promise.reject(new Error('audio ws not open'));
    }
    awaitingFinal = true;

    // 서버는 이 메시지를 받으면 merge → 단 한 번 BINARY emit → complete(또는 그대로 대기) 해야 함
    wsAudio.send(JSON.stringify({ type: 'FINISH' }));

    return new Promise((resolve, reject) => {
        finalResolve = resolve;
        finalReject  = reject;
        setTimeout(() => {
            if (awaitingFinal) {
                awaitingFinal = false;
                reject(new Error('final merge timeout'));
            }
        }, timeoutMs);
    });
}

async function stopAudio() {
    // 1) 녹음 중지 (트랙은 잠시 유지: 최종 결과 받기 전까지 WS 열려 있어야 함)
    if (recorder && recorder.state !== 'inactive') {
        try { recorder.stop(); } catch {}
    }

    // 2) 서버에 최종 병합 요청 → 최종 1건 수신 대기
    let mergeOk = false;
    if (wsAudio && wsAudio.readyState === WebSocket.OPEN) {
        try {
            statusAudio.textContent = 'finalizing…';
            await requestFinalMerge();
            mergeOk = true;
        } catch (e) {
            log('⚠️ [audio] final merge error:', e?.message || e);
        }
    }

    // 3) 이제 소켓 닫기
    if (wsAudio && wsAudio.readyState === WebSocket.OPEN) {
        try { wsAudio.close(1000, mergeOk ? 'final-sent' : 'client-stop'); } catch {}
    }

    // 4) 마무리 정리
    cleanupAudio();
}

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

    // 최종 대기 상태 초기화
    awaitingFinal = false;
    finalResolve = null;
    finalReject  = null;
}

// ---------- Events ----------
btnChatConn.addEventListener('click', connectChat);
btnChatDisc.addEventListener('click', disconnectChat);
btnChatSend.addEventListener('click', sendChat);
chatInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') sendChat();
});

btnAudioStart.addEventListener('click', startAudio);
btnAudioStop.addEventListener('click', stopAudio);

window.addEventListener('beforeunload', () => {
    try { disconnectChat(); } catch {}
    try { stopAudio(); } catch {}
    if (lastObjectUrl) URL.revokeObjectURL(lastObjectUrl);
});
