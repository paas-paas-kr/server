package com.document.application.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 스토리지 서비스 인터페이스
 * 다양한 스토리지 구현체(로컬, NCP, AWS S3 등)를 지원합니다.
 */
public interface StorageService {

    /**
     * 파일을 저장하고 저장된 경로 또는 URL을 반환합니다.
     */
    String store(MultipartFile file);

    /**
     * 저장된 파일의 접근 가능한 URL을 반환합니다.
     */
    String getFileUrl(String fileKey);

    /**
     * 파일을 삭제합니다.
     */
    void delete(String fileKey);
}
