/**
 * 채팅 애플리케이션
 * - 웹소켓을 통한 실시간 채팅
 * - 메시지는 메모리에만 저장 (페이지 새로고침 시 초기화)
 */
(function() {
    // 전역 네임스페이스
    window.ChatApp = window.ChatApp || {};

    // 상태 관리
    let ws = null;
    let wsAudio = null;
    let messages = []; // 메모리에 메시지 저장
    let isConnecting = false;
    let isWaitingResponse = false; // 응답 대기 중
    let currentLang = 'Kor'; // 기본 언어
    const ROOM_ID = 'default_room'; // 단일 채팅방 ID
    let isReconnecting = false; // 재연결 시도 중 플래그

    // DOM 요소
    const elements = {
        welcomeSection: null,
        chatMessagesContainer: null,
        chatMessages: null,
        chatInput: null,
        sendButton: null,
        audioFileInput: null,
        chatInputArea: null,
        loadingMessage: null,
        responseLanguage: null
    };

    /**
     * 초기화
     */
    function init() {
        console.log('🚀 채팅 앱 초기화');

        // DOM 요소 가져오기
        elements.welcomeSection = document.getElementById('welcomeSection');
        elements.chatMessagesContainer = document.getElementById('chatMessagesContainer');
        elements.chatMessages = document.getElementById('chatMessages');
        elements.chatInput = document.getElementById('chatInput');
        elements.sendButton = document.getElementById('sendButton');
        elements.audioFileInput = document.getElementById('audioFileInput');
        elements.chatInputArea = document.getElementById('chatInputArea');
        elements.loadingMessage = document.getElementById('loadingMessage');
        elements.responseLanguage = document.getElementById('responseLanguage');

        // 저장된 메시지 복원
        loadMessages();

        // 사용자 언어 설정 가져오기
        getUserLanguage();

        // 이벤트 리스너 등록
        setupEventListeners();

        // 웹소켓 연결
        connectWebSocket();

        // Marked.js 설정
        setupMarkdown();
    }

    /**
     * 사용자 언어 설정 가져오기
     */
    function getUserLanguage() {
        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            if (userInfo.preferredLanguage) {
                // API 언어 코드를 WebSocket 언어 코드로 변환
                const langMap = {
                    'KOREAN': 'Kor',
                    'ENGLISH': 'Eng',
                    'JAPANESE': 'Jpn',
                    'CHINESE': 'Chn',
                    'VIETNAMESE': 'Vie'
                };
                currentLang = langMap[userInfo.preferredLanguage] || 'Kor';

                // 언어 선택 박스에 반영
                if (elements.responseLanguage) {
                    elements.responseLanguage.value = currentLang;
                }
            }
            console.log('🌐 사용자 언어:', currentLang);
        } catch (e) {
            console.error('언어 설정 로드 실패:', e);
        }
    }

    /**
     * Marked.js 설정
     */
    function setupMarkdown() {
        if (typeof marked === 'undefined') {
            console.warn('Marked.js가 로드되지 않았습니다.');
            return;
        }

        const renderer = new marked.Renderer();
        const originalLinkRenderer = renderer.link;

        // 링크를 새 탭에서 열도록 설정
        renderer.link = function(href, title, text) {
            const html = originalLinkRenderer.call(renderer, href, title, text);
            return html.replace(/^<a /, '<a target="_blank" rel="noopener noreferrer" ');
        };

        marked.setOptions({
            renderer: renderer,
            breaks: true,
            gfm: true
        });
    }

    /**
     * 이벤트 리스너 설정
     */
    function setupEventListeners() {
        // 전송 버튼 클릭
        elements.sendButton.addEventListener('click', sendMessage);

        // 파일 업로드
        elements.audioFileInput.addEventListener('change', handleAudioFileUpload);

        // Enter 키로 전송 (Shift+Enter는 줄바꿈)
        elements.chatInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });

        // textarea 자동 높이 조절
        elements.chatInput.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 200) + 'px';
        });

        // 언어 선택 변경
        if (elements.responseLanguage) {
            elements.responseLanguage.addEventListener('change', function() {
                currentLang = this.value;
                console.log('채팅 응답 언어 변경:', currentLang);
            });
        }

        // 페이지 떠날 때 웹소켓 연결 종료
        window.addEventListener('beforeunload', function() {
            if (ws && ws.readyState === WebSocket.OPEN) {
                ws.close(1000, 'client-close');
            }
        });
    }

    /**
     * 웹소켓 연결
     */
    function connectWebSocket() {
        if (isConnecting || (ws && ws.readyState === WebSocket.OPEN)) {
            console.log('이미 연결 중이거나 연결되어 있습니다.');
            return;
        }

        isConnecting = true;

        // 토큰 가져오기
        const token = localStorage.getItem('accessToken');
        if (!token) {
            console.error('토큰이 없습니다. 로그인이 필요합니다.');
            isConnecting = false;
            return;
        }

        // WebSocket URL 구성 (토큰을 쿼리 파라미터로 전달)
        const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
        const host = window.location.host.replace(':3000', ':8083'); // chat-server 직접 연결
        const wsUrl = `${protocol}://${host}/ws/chat?token=${encodeURIComponent(token)}`;

        console.log('🔌 웹소켓 연결 시도');

        try {
            ws = new WebSocket(wsUrl);
            ws.binaryType = 'arraybuffer';

            ws.onopen = handleOpen;
            ws.onmessage = handleMessage;
            ws.onclose = handleClose;
            ws.onerror = handleError;
        } catch (e) {
            console.error('웹소켓 생성 실패:', e);
            isConnecting = false;
        }
    }

    /**
     * 웹소켓 연결 성공
     */
    function handleOpen() {
        console.log('✅ 웹소켓 연결 성공');
        isConnecting = false;
    }

    /**
     * 웹소켓 메시지 수신
     */
    function handleMessage(event) {
        // 문자열 메시지 (JSON)
        if (typeof event.data === 'string') {
            try {
                const data = JSON.parse(event.data);
                console.log('📩 메시지 수신:', data);

                // 에러 메시지 처리
                if (data.error || (data.type === 'ERROR')) {
                    hideLoadingMessage();

                    // 503 에러인 경우에만 "죄송합니다" 메시지 표시
                    if (data.statusCode === 503 || data.code === 503 ||
                        (data.message && data.message.includes('503'))) {
                        const errorMsg = window.i18n ? window.i18n.translate('error.503') : '죄송합니다. 일시적인 오류가 발생했습니다. 다시 시도해주세요.';
                        addAssistantMessage(errorMsg);
                    }
                    // 503 에러가 아닌 경우 메시지만 표시 (죄송합니다 없이)
                    else {
                        const errorText = data.message || data.error || '오류가 발생했습니다.';
                        console.error('채팅 에러:', errorText);
                        // 503이 아닌 에러는 로그만 남기고 메시지를 표시하지 않음
                    }

                    setWaitingResponse(false);
                    return;
                }

                // 이벤트 타입에 따라 처리
                if (data.event === 'result' || data.event === 'original_text') {
                    // LLM 응답
                    if (data.data && data.data.text) {
                        hideLoadingMessage();
                        addAssistantMessage(data.data.text);
                    }
                } else if (data.type === 'nlp-stream') {
                    // 스트리밍 응답
                    if (data.event === 'original_text' && data.data?.text) {
                        hideLoadingMessage();
                        addAssistantMessage(data.data.text);
                    }
                }
            } catch (e) {
                console.error('JSON 파싱 실패:', e);
            }
        }
        // 바이너리 메시지 (오디오 등)
        else if (event.data instanceof ArrayBuffer) {
            console.log('📦 바이너리 메시지 수신:', event.data.byteLength, 'bytes');
        }
    }

    /**
     * 웹소켓 연결 종료
     */
    function handleClose(event) {
        console.log('🔌 웹소켓 연결 종료:', event.code, event.reason);
        console.log('🔍 연결 종료 상세:', {code: event.code, reason: event.reason, wasClean: event.wasClean});
        isConnecting = false;

        // 비정상 종료 시 로딩 메시지 숨기기
        if (event.code !== 1000) {
            hideLoadingMessage();
            setWaitingResponse(false);
        }

        // 503 에러만 에러 메시지 표시 (1006은 제외)
        // WebSocket close code 1006 = abnormal closure (재연결 시 메시지 표시 안 함)
        if (event.reason.includes('503')) {
            // 에러 메시지 표시 (다국어 지원)
            const errorMsg = window.i18n ? window.i18n.translate('error.503') : '죄송합니다. 일시적인 오류가 발생했습니다. 다시 시도해주세요.';
            const errorMessage = {
                type: 'assistant',
                content: errorMsg,
                timestamp: new Date()
            };
            messages.push(errorMessage);
            renderMessage(errorMessage);
            saveMessages();
        }

        // 재연결 시도
        if (event.code !== 1000) { // 정상 종료가 아니면
            setTimeout(() => {
                console.log('🔄 재연결 시도...');
                connectWebSocket();
            }, 3000);
        }
    }

    /**
     * 웹소켓 에러
     */
    function handleError(error) {
        console.error('❌ 웹소켓 에러:', error);
        isConnecting = false;

        // 로딩 메시지만 숨기고 에러 메시지는 표시하지 않음
        hideLoadingMessage();

        // 입력 활성화
        setWaitingResponse(false);
    }

    /**
     * 메시지 전송
     */
    function sendMessage() {
        if (isWaitingResponse) {
            console.log('응답 대기 중입니다.');
            return;
        }

        if (!ws || ws.readyState !== WebSocket.OPEN) {
            const errorMsg = window.i18n ? window.i18n.translate('error.notConnected') : '채팅 서버에 연결되지 않았습니다.';
            alert(errorMsg);
            return;
        }

        const text = elements.chatInput.value.trim();
        if (!text) return;

        // 현재 선택된 언어 가져오기 (드롭다운에서 직접 읽기)
        if (elements.responseLanguage) {
            currentLang = elements.responseLanguage.value;
        }

        // 환영 섹션 숨기고 채팅 섹션 표시
        showChatSection();

        // 사용자 메시지 표시
        addUserMessage(text);

        // 웹소켓으로 전송
        const message = {
            type: 'CHAT',
            text: text,
            lang: currentLang,
            roomId: ROOM_ID
        };

        console.log('📤 메시지 전송:', message);
        ws.send(JSON.stringify(message));

        // 입력창 초기화
        elements.chatInput.value = '';
        elements.chatInput.style.height = 'auto';

        // 응답 대기 상태로 변경
        setWaitingResponse(true);
    }

    /**
     * 예시 질문 클릭 (하위 호환성 유지)
     */
    function askExample(question) {
        elements.chatInput.value = question;
        sendMessage();
    }

    /**
     * 예시 질문 요소에서 텍스트 읽어서 전송
     */
    function askExampleFromElement(element) {
        const textElement = element.querySelector('.example-question-text');
        if (textElement) {
            const question = textElement.textContent.trim();
            elements.chatInput.value = question;
            sendMessage();
        }
    }

    /**
     * 응답 대기 상태 설정
     */
    function setWaitingResponse(waiting) {
        isWaitingResponse = waiting;

        if (waiting) {
            // 입력 비활성화
            elements.chatInput.disabled = true;
            elements.sendButton.disabled = true;
            elements.chatInputArea.classList.add('loading');

            // 로딩 메시지 표시
            showLoadingMessage();
        } else {
            // 입력 활성화
            elements.chatInput.disabled = false;
            elements.sendButton.disabled = false;
            elements.chatInputArea.classList.remove('loading');
            elements.chatInput.focus();

            // 로딩 메시지 숨기기
            hideLoadingMessage();
        }
    }

    /**
     * 로딩 메시지 표시
     */
    function showLoadingMessage() {
        console.log('🔄 showLoadingMessage 호출됨');
        console.log('elements.loadingMessage:', elements.loadingMessage);

        if (elements.loadingMessage) {
            console.log('✅ 로딩 메시지 요소 존재, active 클래스 추가');
            console.log('현재 클래스:', elements.loadingMessage.className);
            elements.loadingMessage.classList.add('active');
            console.log('변경 후 클래스:', elements.loadingMessage.className);

            // 로딩 메시지가 DOM에 있는지 확인
            if (elements.loadingMessage.parentNode === elements.chatMessages) {
                console.log('✅ 로딩 메시지가 chatMessages에 있음');
            } else {
                console.log('⚠️ 로딩 메시지가 chatMessages에 없음, 다시 추가');
                elements.chatMessages.appendChild(elements.loadingMessage);
            }

            scrollToBottom();
        } else {
            console.error('❌ loadingMessage 요소를 찾을 수 없습니다');
        }
    }

    /**
     * 로딩 메시지 숨기기
     */
    function hideLoadingMessage() {
        if (elements.loadingMessage) {
            console.log('✅ 로딩 메시지 숨김');
            elements.loadingMessage.classList.remove('active');
        }
    }

    /**
     * 채팅 메시지 컨테이너 표시
     */
    function showChatSection() {
        // 환영 섹션은 숨기기
        if (elements.welcomeSection) {
            elements.welcomeSection.style.display = 'none';
        }
        // 메시지 컨테이너 표시
        if (elements.chatMessagesContainer) {
            elements.chatMessagesContainer.classList.add('active');
        }
    }

    /**
     * 사용자 메시지 추가
     */
    function addUserMessage(text) {
        const message = {
            type: 'user',
            content: text,
            timestamp: new Date()
        };
        messages.push(message);
        renderMessage(message);
        saveMessages();
    }

    /**
     * AI 응답 메시지 추가
     */
    function addAssistantMessage(text) {
        const message = {
            type: 'assistant',
            content: text,
            timestamp: new Date()
        };
        messages.push(message);
        renderMessage(message);
        saveMessages();

        // 응답 완료
        setWaitingResponse(false);
    }

    /**
     * 메시지 렌더링
     */
    function renderMessage(message) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${message.type}`;

        // 메시지 래퍼
        const wrapper = document.createElement('div');
        wrapper.className = 'message-wrapper';

        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';

        // Markdown 렌더링 (assistant 메시지만)
        if (message.type === 'assistant' && typeof marked !== 'undefined') {
            const htmlContent = marked.parse(message.content);
            contentDiv.innerHTML = `<div class="markdown-content">${htmlContent}</div>`;
        } else {
            contentDiv.textContent = message.content;
        }

        wrapper.appendChild(contentDiv);

        // 타임스탬프 추가
        const timeDiv = document.createElement('div');
        timeDiv.className = 'message-time';
        timeDiv.textContent = formatTime(message.timestamp);
        wrapper.appendChild(timeDiv);

        messageDiv.appendChild(wrapper);

        // 로딩 메시지 전에 삽입
        if (elements.loadingMessage && elements.loadingMessage.parentNode === elements.chatMessages) {
            elements.chatMessages.insertBefore(messageDiv, elements.loadingMessage);
        } else {
            elements.chatMessages.appendChild(messageDiv);
        }

        scrollToBottom();
    }

    /**
     * 스크롤을 아래로
     */
    function scrollToBottom() {
        setTimeout(() => {
            if (elements.chatMessagesContainer) {
                elements.chatMessagesContainer.scrollTop = elements.chatMessagesContainer.scrollHeight;
            }
        }, 100);
    }

    /**
     * 시간 포맷팅
     */
    function formatTime(date) {
        const hours = date.getHours().toString().padStart(2, '0');
        const minutes = date.getMinutes().toString().padStart(2, '0');
        return `${hours}:${minutes}`;
    }

    /**
     * 오디오 파일 업로드 처리
     */
    async function handleAudioFileUpload(event) {
        const file = event.target.files[0];
        if (!file) return;

        console.log('📁 오디오 파일 선택:', file.name, file.size, 'bytes');

        // 환영 섹션 숨기고 채팅 섹션 표시
        showChatSection();

        // 음성 파일 첨부 메시지 표시
        addUserMessage(`음성 파일 첨부: ${file.name}`);

        // 파일 업로드 중 표시
        setWaitingResponse(true);

        try {
            // 오디오 WebSocket 연결
            await connectAudioWebSocket();

            // 메타데이터 전송
            const meta = {
                type: 'START',
                lang: elements.responseLanguage ? elements.responseLanguage.value : currentLang,
                mimeType: file.type || 'audio/webm',
                roomId: ROOM_ID
            };
            wsAudio.send(JSON.stringify(meta));
            console.log('📤 오디오 메타데이터 전송:', meta);

            // 파일을 청크로 나누어 전송
            const chunkSize = 8192; // 8KB 청크
            const arrayBuffer = await file.arrayBuffer();
            const totalChunks = Math.ceil(arrayBuffer.byteLength / chunkSize);

            for (let i = 0; i < totalChunks; i++) {
                const start = i * chunkSize;
                const end = Math.min(start + chunkSize, arrayBuffer.byteLength);
                const chunk = arrayBuffer.slice(start, end);

                // 시퀀스 번호와 함께 전송
                const payload = new Uint8Array(chunk);
                const message = new Uint8Array(4 + payload.length);
                const view = new DataView(message.buffer);
                view.setUint32(0, i); // Big-endian 시퀀스 번호
                message.set(payload, 4);

                wsAudio.send(message);
                console.log(`📤 오디오 청크 전송: ${i + 1}/${totalChunks}, size=${payload.length}B`);

                // 네트워크 부하 방지를 위한 짧은 대기
                if (i < totalChunks - 1) {
                    await new Promise(resolve => setTimeout(resolve, 10));
                }
            }

            // FINISH 메시지 전송
            wsAudio.send(JSON.stringify({ type: 'FINISH' }));
            console.log('📤 FINISH 전송');

        } catch (error) {
            console.error('오디오 파일 전송 실패:', error);
            const errorMsg = window.i18n ? window.i18n.translate('error.audioUpload') : '오디오 파일 전송 중 오류가 발생했습니다.';
            alert(errorMsg);
            setWaitingResponse(false);
        } finally {
            // 파일 입력 초기화
            event.target.value = '';
        }
    }

    /**
     * 오디오 WebSocket 연결
     */
    function connectAudioWebSocket() {
        return new Promise((resolve, reject) => {
            if (wsAudio && wsAudio.readyState === WebSocket.OPEN) {
                resolve();
                return;
            }

            // 토큰 가져오기
            const token = localStorage.getItem('accessToken');
            if (!token) {
                reject(new Error('토큰이 없습니다.'));
                return;
            }

            // WebSocket URL 구성
            const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
            const host = window.location.host.replace(':3000', ':8083');
            const wsUrl = `${protocol}://${host}/ws/audio?token=${encodeURIComponent(token)}`;

            console.log('🔌 오디오 WebSocket 연결 시도');

            wsAudio = new WebSocket(wsUrl);
            wsAudio.binaryType = 'arraybuffer';

            wsAudio.onopen = function() {
                console.log('✅ 오디오 WebSocket 연결 성공');
                resolve();
            };

            wsAudio.onmessage = function(event) {
                if (typeof event.data === 'string') {
                    try {
                        const data = JSON.parse(event.data);
                        console.log('📩 오디오 응답 수신:', data);

                        // 로딩 메시지 숨기기
                        hideLoadingMessage();

                        if (data.type === 'SYSTEM') {
                            // 시스템 메시지
                            console.log('시스템:', data.text);
                        } else if (data.event === 'result' || data.event === 'original_text') {
                            // LLM 응답
                            if (data.data && data.data.text) {
                                addAssistantMessage(data.data.text);
                            }
                        }
                    } catch (e) {
                        console.error('JSON 파싱 실패:', e);
                    }
                }
            };

            wsAudio.onclose = function(event) {
                console.log('🔌 오디오 WebSocket 연결 종료:', event.code, event.reason);
                wsAudio = null;
            };

            wsAudio.onerror = function(error) {
                console.error('❌ 오디오 WebSocket 에러:', error);
                reject(error);
            };
        });
    }

    /**
     * 메시지 저장
     */
    function saveMessages() {
        try {
            localStorage.setItem('chatMessages', JSON.stringify(messages));
        } catch (e) {
            console.error('메시지 저장 실패:', e);
        }
    }

    /**
     * 메시지 로드
     */
    function loadMessages() {
        try {
            const saved = localStorage.getItem('chatMessages');
            if (saved) {
                messages = JSON.parse(saved);
                console.log('💾 저장된 메시지 복원:', messages.length + '개');

                // 메시지가 있으면 채팅 화면 표시
                if (messages.length > 0) {
                    showChatSection();
                    messages.forEach(msg => {
                        msg.timestamp = new Date(msg.timestamp);
                        renderMessage(msg);
                    });

                    // 로딩 메시지를 맨 뒤로 이동
                    if (elements.loadingMessage && elements.chatMessages) {
                        elements.chatMessages.appendChild(elements.loadingMessage);
                    }
                }
            }
        } catch (e) {
            console.error('메시지 로드 실패:', e);
        }
    }

    /**
     * 메시지 초기화
     */
    function clearMessages() {
        const confirmMsg = window.i18n ? window.i18n.translate('error.confirm.newChat') : '새 채팅을 시작하시겠습니까? 현재 대화 내용이 삭제됩니다.';
        if (!confirm(confirmMsg)) {
            return;
        }

        messages = [];
        localStorage.removeItem('chatMessages');
        elements.chatMessages.innerHTML = '<div class="loading-message" id="loadingMessage"><div class="loading-icon"><svg viewBox="0 0 24 24" fill="white" style="width: 24px; height: 24px;"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22" fill="white"></polyline></svg></div></div>';
        elements.loadingMessage = document.getElementById('loadingMessage');
        elements.welcomeSection.style.display = 'block';
        elements.chatMessagesContainer.classList.remove('active');
    }

    // 공개 API
    window.ChatApp.init = init;
    window.ChatApp.sendMessage = sendMessage;
    window.ChatApp.askExample = askExample;
    window.ChatApp.askExampleFromElement = askExampleFromElement;
    window.ChatApp.connect = connectWebSocket;
    window.ChatApp.clearMessages = clearMessages;
})();
