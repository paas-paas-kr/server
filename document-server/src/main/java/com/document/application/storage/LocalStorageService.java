package com.document.application.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.common.exception.document.DocumentErrorCode;
import com.common.exception.document.DocumentException;

/**
 * 로컬 파일 시스템 스토리지 구현체
 */
@Slf4j
@Service
@Profile("local")
public class LocalStorageService implements StorageService {

    private final Path uploadPath;

    public LocalStorageService(@Value("${document.upload.path:/tmp/documents}") String uploadPath) {
        this.uploadPath = Paths.get(uploadPath);
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException e) {
            throw DocumentException.from(DocumentErrorCode.FILE_STORAGE_INITIALIZATION_FAILED);
        }
    }

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

        // 날짜별 디렉토리 생성 (예: 2025-10-15)
        String dateDir = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        Path datePath = uploadPath.resolve(dateDir);

        try {
            Files.createDirectories(datePath);

            // 고유한 파일명 생성
            String fileName = UUID.randomUUID() + extension;
            Path destinationFile = datePath.resolve(fileName);

            // 파일 저장
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            // 저장된 경로 반환 (상대 경로)
            String savedPath = dateDir + "/" + fileName;
            return savedPath;
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", originalFilename, e);
            throw DocumentException.from(DocumentErrorCode.DOCUMENT_PROCESSING_ERROR);
        }
    }

    @Override
    public String getFileUrl(String relativePath) {
        // 로컬 파일 시스템의 경우 절대 경로를 반환
        return uploadPath.resolve(relativePath).toString();
    }

    @Override
    public void delete(String relativePath) {
        try {
            Path filePath = uploadPath.resolve(relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", relativePath, e);
            throw DocumentException.from(DocumentErrorCode.DOCUMENT_PROCESSING_ERROR);
        }
    }

    /**
     * 저장된 파일의 절대 경로를 반환합니다.
     */
    public Path getAbsolutePath(String relativePath) {
        return uploadPath.resolve(relativePath);
    }
}
