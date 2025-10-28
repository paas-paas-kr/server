FROM eclipse-temurin:17-jdk-jammy

# Static FFmpeg 바이너리 다운로드 (apt-get 불필요)
ADD https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz /tmp/ffmpeg.tar.xz
RUN cd /tmp && \
    tar xf ffmpeg.tar.xz && \
    mv ffmpeg-*-amd64-static/ffmpeg /usr/local/bin/ffmpeg && \
    chmod +x /usr/local/bin/ffmpeg && \
    rm -rf /tmp/ffmpeg*

# FFmpeg 설치 확인
RUN ffmpeg -version

# 애플리케이션 JAR 복사
COPY build/libs/*SNAPSHOT.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]