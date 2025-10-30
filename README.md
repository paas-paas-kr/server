<div align="center">

#  온가: 온 세상 가족

</div>

## 📋 목차

- [📖 프로젝트 개요](#-프로젝트-개요)
- [✨ 주요 기능](#-주요-기능)
- [⚙️ 기술 스택](#️-기술-스택)
- [🏗️ 시스템 아키텍처](#️-시스템-아키텍처)
- [🧑‍🤝‍🧑 팀](#️-팀)


---

# 📖 프로젝트 개요

언어와 문화의 차이는 때로는 정보의 벽이 되기도 합니다. 특히 다문화 가정의 경우, 필요한 행정 절차나 복지 정보를 찾더라도 낯선 언어와 복잡한 문서 형식 때문에 이해하기 어려운 상황이 많습니다. 

이 문제를 해결하기 위해, 우리는 다문화 가정을 위한 AI 정보 지원 플랫폼을 만들었습니다. 사용자가 모국어로 질문하면 AI가 실시간으로 번역·이해하고, 정부 및 공공 데이터를 기반으로 정확하고 신뢰할 수 있는 정보를 제공합니다. 

또한 공문서나 가정통신문, 안내문 등 이해하기 어려운 한국어 문서와 이미지를 AI가 인식해 쉽고 친절한 언어로 해석해주는 기능도 제공합니다.

다문화 가정이 한국 사회에서 언어의 장벽 없이 필요한 정보를 얻고, 스스로 문제를 해결할 수 있는 힘을 가지는 것. 그것이 우리가 이 프로젝트를 시작한 이유입니다.


## ✨ 주요 기능

- **🤖 AI 채팅**: 음성/텍스트 다국어 질의응답으로 20+ 공공기관 데이터를 검색하고 출처 기반 답변 제공
- **📄 문서 요약**: 공문서·가정통신문을 OCR로 읽고 개요/할 일/일정/주의사항을 자동 정리
- **🌐 다국어 지원**: 선호 언어 설정 시 모든 UI와 AI 응답이 자동 번역되어 제공
- **🔍 지능형 검색**: Gemini 의도 분석과 Vertex AI Search로 정확한 정보 탐색

---

# ⚙️ 기술 스택

| **분류** | **기술 스택** |
|:--|:--|
| **Backend Framework** | ![Java](https://img.shields.io/badge/Java%2017-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white) ![Spring WebFlux](https://img.shields.io/badge/Spring%20WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white) ![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white) |
| **Infrastructure** | ![Kubernetes](https://img.shields.io/badge/Kubernetes%20(NKS)-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white) ![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white) |
| **DevOps** | ![NCP SourceCommit](https://img.shields.io/badge/NCP%20SourceCommit-03C75A?style=for-the-badge&logo=naver&logoColor=white) ![NCP SourceBuild](https://img.shields.io/badge/NCP%20SourceBuild-03C75A?style=for-the-badge&logo=naver&logoColor=white) ![NCP SourceDeploy](https://img.shields.io/badge/NCP%20SourceDeploy-03C75A?style=for-the-badge&logo=naver&logoColor=white) ![NCP SourcePipeline](https://img.shields.io/badge/NCP%20SourcePipeline-03C75A?style=for-the-badge&logo=naver&logoColor=white) |
| **Database & Storage** | ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white) ![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white) ![Google Firestore](https://img.shields.io/badge/Google%20Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=111) ![Naver Object Storage](https://img.shields.io/badge/Naver%20Object%20Storage-03C75A?style=for-the-badge&logo=naver&logoColor=white) |
| **AI** | ![Vertex AI Search](https://img.shields.io/badge/Vertex%20AI%20Search-4285F4?style=for-the-badge&logo=googlecloud&logoColor=white) ![Google Gemini AI](https://img.shields.io/badge/Google%20Gemini%20AI-8E75FF?style=for-the-badge&logo=google&logoColor=white) ![OpenAI](https://img.shields.io/badge/OpenAI-412991?style=for-the-badge&logo=openai&logoColor=white) |
| **Open API** | ![Naver CLOVA OCR](https://img.shields.io/badge/Naver%20CLOVA%20OCR-03C75A?style=for-the-badge&logo=naver&logoColor=white) ![Naver CSR](https://img.shields.io/badge/Naver%20CSR-03C75A?style=for-the-badge&logo=naver&logoColor=white) ![Naver Papago](https://img.shields.io/badge/Naver%20Papago-03C75A?style=for-the-badge&logo=naver&logoColor=white) |
| **Development Tools** | ![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-000000?style=for-the-badge&logo=intellijidea&logoColor=white) ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=white) ![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white) ![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white) |

---

# 🏗️ 시스템 아키텍처

## MSA
<p align="center">
  <img width="2477" height="1392" alt="image" src="https://github.com/user-attachments/assets/d71254e1-acf9-4f7b-a899-1e7b0ef4f2f6" />
  <br/><i>Kubernetes 기반 멀티 모듈 MSA. Ingress → Gateway → Services 구조.</i>
</p>

<br>

- 네트워킹/라우팅: Ingress → Gateway → 각 마이크로서비스  
- 데이터 저장소: MySQL, Redis, Firestore (PV/PVC 영속 관리)  
- 배포: NCP SourceCommit → SourceBuild → SourceDeploy → SourcePipeline  
- 각 모듈 독립 빌드/배포/롤백 지원  


## 채팅

<p align="center">
  <img width="1689" height="741" alt="image" src="https://github.com/user-attachments/assets/3342556c-e092-43b9-974c-e53f0c37fa9a" />
</p>

1. 게이트웨이 서버를 통해 채팅 서버로 요청 전달
2. 음성 데이터를 Clova STT로 텍스트 변환
3. Papago로 사용자 언어 → 한국어 번역
4. Gemini가 질문 의도 파악 및 검색 쿼리 추출
5. Vertex AI Search로 관련 콘텐츠 검색
6. 검색 결과와 프롬프트를 Gemini에 전달해 답변 생성
7. Papago로 한국어 → 사용자 언어 번역
8. 질문과 답변을 Firestore에 저장
9. 최종 응답을 사용자에게 전달


## 문서 시스템

<p align="center">
  <img width="2188" height="1170" alt="image" src="https://github.com/user-attachments/assets/2b1c6a43-ed64-4e21-8cdd-288e563f01ae" />
</p>

1. 문서 업로드 → Object Storage  
2. Redis Job 발행 → Consumer 비동기 처리  
3. 이미지형 문서 → OCR 처리  
4. OpenAI 프롬프트 요약 → MySQL 저장  
5. 마이페이지에서 요약 결과 및 이력 조회

<p align="center">
  <img width="2142" height="571" alt="image" src="https://github.com/user-attachments/assets/59342834-cccd-44b2-8f78-016d5d788a18" />
</p>


## 게이트웨이

<p align="center">
  <img width="1323" height="521" alt="image" src="https://github.com/user-attachments/assets/9c9a633e-9c3b-443d-b73f-4a078e1b0da4" />
</p>

- Gateway: JWT 인증/인가 및 요청 라우팅
- Member: 사용자 프로필 / 선호 언어 설정 관리
- 각 서비스 간 통신은 Service 이름 기반 내부 DNS 활용

---

# 🧑‍🤝‍🧑 팀

| **김기민** | **정찬민** |
|:-----------:|:-----------:|
| <img src="https://github.com/gimin0226.png" height="150" width="150"> | <img src="https://github.com/chanmin-00.png" height="150" width="150"> |
| [@gimin0226](https://github.com/gimin0226) | [@chanmin-00](https://github.com/chanmin-00) |
| 숭실대학교 소프트웨어학부 | 숭실대학교 소프트웨어학부 |



