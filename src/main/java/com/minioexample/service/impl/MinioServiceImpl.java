package com.minioexample.service.impl;

import com.minioexample.entity.FileInfo;
import com.minioexample.repository.FileInfoRepository;
import com.minioexample.service.MinioService;
import io.minio.*;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;

@Service
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final FileInfoRepository fileInfoRepository;
    @Value("${minio.bucket.name}")
    String bucketName;
    public MinioServiceImpl(MinioClient minioClient, FileInfoRepository fileInfoRepository) {
        this.minioClient = minioClient;
        this.fileInfoRepository = fileInfoRepository;
    }


    @Override
    public void uploadFile(MultipartFile file) throws IOException {
        try {
            // Generate unique file name or use the original file name based on your requirement
            String fileName = file.getOriginalFilename();

            // Upload file to Minio
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName) // Replace with your bucket name
                            .object(fileName)
                            .contentType(file.getContentType())
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build()
            );

            // Save file details to the database
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(fileName);
            fileInfo.setUploadDate(new Date());
            // Set other file details as needed
            fileInfoRepository.save(fileInfo);

        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | MinioException e) {
            // Handle exceptions
            e.printStackTrace();
            throw new RuntimeException("Failed to upload file to Minio");
        }
    }

    @Override
    public ResponseEntity<Resource> downloadFile(Long fileId) {
        Optional<FileInfo> fileInfoOptional = fileInfoRepository.findById(fileId);
        if (fileInfoOptional.isPresent()) {
            FileInfo fileInfo = fileInfoOptional.get();
            try {
                // Get object info to obtain content length
                StatObjectResponse statObjectResponse = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileInfo.getFileName())
                                .build()
                );

                // Download file from Minio
                InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileInfo.getFileName())
                                .build()
                );

                // Return the file as a Resource
                InputStreamResource resource = new InputStreamResource(inputStream);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileInfo.getFileName())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .contentLength(statObjectResponse.size())
                        .body(resource);

            } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | MinioException e) {
                // Handle exceptions
                e.printStackTrace();
                throw new RuntimeException("Failed to download file from Minio");
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @Override
    public void deleteFile(Long fileId) {
        Optional<FileInfo> fileInfoOptional = fileInfoRepository.findById(fileId);
        if (fileInfoOptional.isPresent()) {
            FileInfo fileInfo = fileInfoOptional.get();
            try {
                // Delete file from Minio
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName) // Replace with your bucket name
                                .object(fileInfo.getFileName())
                                .build()
                );

                // Delete file details from the database
                fileInfoRepository.deleteById(fileId);

            } catch (InvalidKeyException | NoSuchAlgorithmException | MinioException | IOException e) {
                // Handle exceptions
                e.printStackTrace();
                throw new RuntimeException("Failed to delete file from Minio");
            }
        }
    }
}
