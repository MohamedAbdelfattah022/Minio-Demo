package com.mohamed.miniodemo.service;

import com.mohamed.miniodemo.config.MinioProperties;
import com.mohamed.miniodemo.entity.FileMetadata;
import com.mohamed.miniodemo.repository.FileMetadataRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {
    private final MinioClient minioClient;
    private final MinioProperties props;
    private final FileMetadataRepository fileMetadataRepository;

    @Transactional
    public FileMetadata uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File must not be null or empty");

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty())
            throw new IllegalArgumentException("File name cannot be null or empty");

        UUID fileId = UUID.randomUUID();
        String fileExtension = getFileExtension(originalFilename);
        String storedFileName = fileId + (fileExtension.isEmpty() ? "" : "." + fileExtension);

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(storedFileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            FileMetadata fileMetadata = FileMetadata.builder()
                    .originalFilename(originalFilename)
                    .storedFilename(storedFileName)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .uploadedAt(LocalDateTime.now())
                    .bucketName(props.getBucket())
                    .build();

            fileMetadataRepository.save(fileMetadata);

            log.info("File '{}' uploaded successfully with UUID '{}' to bucket '{}'",
                    originalFilename, fileId, props.getBucket());

            return fileMetadata;
        } catch (MinioException e) {
            log.error("MinIO error uploading file '{}': {}", originalFilename, e.getMessage(), e);
            throw new RuntimeException("Error uploading file to MinIO: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error uploading file '{}': {}", originalFilename, e.getMessage(), e);
            throw new RuntimeException("Error uploading file to MinIO", e);
        }
    }

    @Transactional
    public InputStream downloadFile(UUID fileId) {
        if (fileId == null)
            throw new IllegalArgumentException("File ID cannot be null");

        var fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File metadata not found for ID: " + fileId));

        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(fileMetadata.getBucketName())
                            .object(fileMetadata.getStoredFilename())
                            .build()
            );

            log.info("File '{}' (ID: {}) downloaded successfully from bucket '{}'",
                    fileMetadata.getOriginalFilename(), fileId, props.getBucket());
            return inputStream;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                log.warn("File with ID '{}' not found in bucket '{}'", fileId, props.getBucket());
                throw new RuntimeException("File not found in storage: " + fileId, e);
            }
            log.error("MinIO error downloading file ID '{}': {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Error downloading file from MinIO: " + e.getMessage(), e);
        } catch (MinioException e) {
            log.error("MinIO error downloading file ID '{}': {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Error downloading file from MinIO: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error downloading file ID '{}': {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Error downloading file from MinIO", e);
        }
    }

    @Transactional
    public void deleteFile(UUID fileId) {
        if (fileId == null)
            throw new IllegalArgumentException("File ID cannot be null");

        var fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File metadata not found for ID: " + fileId));

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(fileMetadata.getStoredFilename())
                            .build()
            );

            fileMetadataRepository.deleteById(fileId);

            log.info("File '{}' (ID: {}) deleted successfully from bucket '{}'",
                    fileMetadata.getOriginalFilename(), fileId, props.getBucket());
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                log.warn("File with ID '{}' not found in bucket '{}', deleting metadata anyway",
                        fileId, props.getBucket());
                fileMetadataRepository.deleteById(fileId);
                return;
            }
            log.error("MinIO error deleting file ID '{}': {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Error deleting file from MinIO: " + e.getMessage(), e);
        } catch (MinioException e) {
            log.error("MinIO error deleting file ID '{}': {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Error deleting file from MinIO: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error deleting file ID '{}': {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Error deleting file from MinIO", e);
        }
    }

    private String getFileExtension(String originalFilename) {
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(lastDotIndex + 1);
    }

    public List<FileMetadata> listAllFiles() {
        return fileMetadataRepository.findAll();
    }

    public FileMetadata getFileMetadata(UUID fileId) {
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));
    }

}