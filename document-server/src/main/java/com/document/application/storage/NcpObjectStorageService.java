package com.document.application.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.common.exception.document.DocumentErrorCode;
import com.common.exception.document.DocumentException;

/**
 * NCP 객체 스토리지 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!local")
public class NcpObjectStorageService implements StorageService {

    private final S3Client s3Client;

    @Value("${ncp.object-storage.bucket}")
    private String bucketName;

    @Value("${ncp.object-storage.public-url:}")
    private String publicUrl;

    @Override
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw DocumentException.from(DocumentErrorCode.DOCUMENT_PROCESSING_ERROR);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 날짜별 디렉토리 구조 (예: 2025-10-15)
        String dateDir = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        // 고유한 파일명 생성
        String fileName = UUID.randomUUID() + extension;
        String objectKey = dateDir + "/" + fileName;

        try {
            // S3에 파일 업로드 (public-read 권한으로 설정)
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            if (publicUrl != null && !publicUrl.isEmpty()) {
                return publicUrl + "/" + objectKey;
            }
            return objectKey;

        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", originalFilename, e);
            throw DocumentException.from(DocumentErrorCode.DOCUMENT_PROCESSING_ERROR);
        } catch (S3Exception e) {
            log.error("S3 업로드 실패: {}", originalFilename, e);
            throw DocumentException.from(DocumentErrorCode.DOCUMENT_PROCESSING_ERROR);
        }
    }

    @Override
    public String getFileUrl(String objectKey) {
        return objectKey;
    }

    @Override
    public void delete(String objectKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            log.error("파일 삭제 실패: {}", objectKey, e);
            throw DocumentException.from(DocumentErrorCode.DOCUMENT_PROCESSING_ERROR);
        }
    }
}
