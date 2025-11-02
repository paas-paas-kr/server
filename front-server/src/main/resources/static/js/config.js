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

    // 토큰 유효성 검증 함수
    window.validateToken = async function() {
        const token = localStorage.getItem('accessToken');
        const isLoggedIn = localStorage.getItem('isLoggedIn');

        // 로그인 상태가 아니면 false 반환
        if (!isLoggedIn || isLoggedIn !== 'true' || !token) {
            return false;
        }

        try {
            console.log('🔍 토큰 유효성 검증 중...');

            // /api/auth/me 엔드포인트로 토큰 유효성 확인
            const response = await fetch(`${window.API_BASE_URL}/api/auth/me`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                },
                credentials: 'include'
            });

            if (response.status === 401 || response.status === 403) {
                // 토큰 만료 또는 유효하지 않음 - 자동 로그아웃
                console.warn('⚠️ 토큰이 만료되었거나 유효하지 않습니다.');
                localStorage.removeItem('accessToken');
                localStorage.removeItem('isLoggedIn');
                localStorage.removeItem('userInfo');
                return false;
            }

            if (!response.ok) {
                console.error('❌ 토큰 검증 실패:', response.status);
                return false;
            }

            console.log('✅ 토큰 유효성 검증 완료');
            return true;
        } catch (error) {
            console.error('❌ 토큰 검증 중 오류:', error);
            // 네트워크 오류의 경우 false 반환하지 않음 (서버 다운 등의 경우)
            return false;
        }
    };

    // 인증 페이지로 리다이렉트하는 함수
    window.redirectToLogin = function(message) {
        if (message) {
            alert(message);
        }
        window.location.href = '/auth/login';
    };
})();
