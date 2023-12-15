package com.minioexample.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface MinioService {
    void uploadFile(MultipartFile file) throws IOException;

    ResponseEntity<Resource> downloadFile(Long fileId);

    void deleteFile(Long fileId);
}
