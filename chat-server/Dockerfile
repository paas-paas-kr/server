FROM eclipse-temurin:17-jdk-jammy

# FFmpeg 설치 (음성 처리용)
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# FFmpeg 설치 확인
RUN ffmpeg -version

# 애플리케이션 JAR 복사
COPY build/libs/*SNAPSHOT.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]