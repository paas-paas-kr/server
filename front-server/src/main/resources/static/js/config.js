// API 설정 - 환경별 API Base URL
(function() {
    const hostname = window.location.hostname;
    const port = window.location.port;

    // 로컬 개발 환경 감지
    const isLocal = (hostname === 'localhost' || hostname === '127.0.0.1') && port === '3000';

    if (isLocal) {
        // 로컬: Gateway 서버로 직접 호출 (localhost:8080)
        window.API_BASE_URL = 'http://localhost:8080';
        console.log('🔧 Local Development Mode');
        console.log('   Front Server: http://localhost:3000');
        console.log('   Gateway (API): http://localhost:8080');
    } else {
        // 프로덕션: Ingress를 통한 상대 경로
        window.API_BASE_URL = '';
        console.log('🚀 Production Mode - Using Ingress (same domain)');
    }

    console.log('API Base URL:', window.API_BASE_URL || 'relative path');

    // API 요청 시 사용할 헤더 생성 함수
    window.getAuthHeaders = function() {
        const token = localStorage.getItem('accessToken');
        const headers = {
            'Content-Type': 'application/json'
        };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    };
})();
