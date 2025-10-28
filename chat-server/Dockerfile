FROM openjdk:17-jdk-alpine

RUN apk add --no-cache ffmpeg

COPY build/libs/*SNAPSHOT.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]