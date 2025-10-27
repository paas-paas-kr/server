package com.chat.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Configuration
public class FirestoreConfig {

    @Value("${app.gcp.project-id}")   // ← application.yml과 경로 일치!
    private String projectId;

    @Value("${app.gcp.sa.path}")      // ← 절대경로(예: C:/keys/xxx.json)
    private String serviceAccountPath;

    @Bean
    public Firestore firestore() throws IOException {
        // mTLS 강제 비활성화 (예전 에러 방지)
        System.setProperty("GOOGLE_API_USE_CLIENT_CERTIFICATE", "false");
        System.setProperty("GOOGLE_API_USE_MTLS_ENDPOINT", "never");

        Objects.requireNonNull(projectId, "app.gcp.project-id is null");
        Objects.requireNonNull(serviceAccountPath, "app.gcp.sa.path is null");

        try (FileInputStream in = new FileInputStream(serviceAccountPath)) {
            GoogleCredentials creds = GoogleCredentials.fromStream(in)
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

            return FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .setDatabaseId("paas-paas-database")
                    .setCredentials(creds)
                    .build()
                    .getService();
        }
    }
}
