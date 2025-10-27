package com.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 구글 API( 특히 Vertex AI) 호출에 필요한 액세스 토큰을 효율적이고 안전하게 관리하는 스프링 컴포넌트
 * com.google.auth.oauth2는 자바 애플리케이션이 Vertex AI, Google Cloud Storage, BigQuery 등 다양한 구글 API 및 서비스에 접근할 때
 * 필요한 인증(Authentication)과 인가(Authorization)를 처리하는 클래스들을 모아놓은 곳
 * 주요 클래스
 * com.google.auth.oauth2.AccessToken : API 호출에 사용되는 실제 액세스 토큰 문자열과 만료 시간 정보를 담고 있다.
 * com.google.auth.oauth2.GoogleCredentials: 서비스 계정 키(JSON) 파일이나 환경 변수 등 다양한 방법으로 인증 정보를 로드하는 역할
 */
@Component
@RequiredArgsConstructor
public class GoogleAccessTokenProvider {
    private final AppProperties props;
    private volatile com.google.auth.oauth2.AccessToken cached;


    //외부에서 토큰을 요청할 때 호출하는 메서드
    public synchronized String getBearerToken() {
        try {
            // 캐시된 토큰이 없거나 토큰의 만료 시간이 (현재 시간+60초)보다 이전일 경우(만료 시간이 60초 이내로 임박) 토큰을 새로 갱신
            // 60초의 여유 시간을 두는 이유는, 토큰을 받아서 API를 호출하려는 그 짧은 순간에 토큰이 만료되는 상황을 방지하기 위해서
            if (cached == null || cached.getExpirationTime().toInstant().isBefore(java.time.Instant.now().plusSeconds(60))) {

                // getApplicationDefault(): 모든 권한이 담긴 서비스 계정 키(JSON 파일) 경로로 호출 (마스터키(json파일)을 보여주어 인증하는 것)
                // -> PC의 환경 변수에 GOOGLE_APPLICATION_CREDENTIALS라는 이름의 환경 변수가 있는지 찾는다 -> 변수가 있다면, 그 값(JSON 파일 경로)을 읽어 마스터 키로 사용
                // -> *** 배포 환경에서는 GCP 서버(Cloud Run, GKE 등)에서 돌리고 GCP 내부의 인증 서버에 자동으로 접속을 시도 ***
                // createScoped(): 로드한 인증 정보에 특정 권한 범위(Scope)를 부여 (A방(Vertex AI)만 갈거라고 인가 범위를 요청하는 것)
                // -> Vertex AI 사용자, BigQuery 관리자, Storage 관리자 등 GCP 콘솔(IAM)에서 이 서비스 계정에 여러 권한을 부여할 수 있다.
                // -> 내가 하려는 일은 Vertex AI API 호출뿐이므로 Vertex AI API 전용 1회용 권한을 받아서 Vertex AI Search를 호출하는 것이다.
                // --> 마스터 키(json파일)을 들고 다니다가 Vertex AI API에서 뺐기면 BigQuery, Storage등 마음대로 드나들 수 있다.
                // --> 1회용 액세스 토큰을 받으면 토큰을 뺐기게 되어도 기껏해야 Vertex AI API만 접근할 수 있고, 특정 시간이 지나면 만료된다. 따라서 피해가 최소화된다.
                var creds = com.google.auth.oauth2.GoogleCredentials.getApplicationDefault()
                        .createScoped(List.of(props.getVertex().getScope()));
                // 인증 정보 자체를 갱신
                creds.refreshIfExpired();
                // 실제로 구글 인증 서버와 네트워크 통신을 수행하여, 위에서 준비한 인증 정보를 보내고 새로운 액세스 토큰을 발급 받는다. (일회용, 마스터카드 아님)
                cached = creds.getAccessToken();
            }
            return cached.getTokenValue();
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain Google access token", e);
        }
    }
}
