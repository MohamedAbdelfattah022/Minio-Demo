package com.mohamed.miniodemo.controller;

import com.mohamed.miniodemo.dto.FileMetadataResponse;
import com.mohamed.miniodemo.dto.FileUploadResponse;
import com.mohamed.miniodemo.entity.FileMetadata;
import com.mohamed.miniodemo.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("files")
@RequiredArgsConstructor
public class MinioController {
    private final MinioService minioService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty())
                return ResponseEntity.badRequest().build();

            var fileMetadata = minioService.uploadFile(file);

            var response = FileUploadResponse.builder()
                    .id(fileMetadata.getId())
                    .originalFilename(fileMetadata.getOriginalFilename())
                    .storedFilename(fileMetadata.getStoredFilename())
                    .fileSize(fileMetadata.getFileSize())
                    .contentType(fileMetadata.getContentType())
                    .uploadedAt(fileMetadata.getUploadedAt())
                    .message("File uploaded successfully")
                    .build();


            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{fileId}/metadata")
    public ResponseEntity<FileMetadataResponse> getFileMetadata(@PathVariable UUID fileId) {
        try {
            FileMetadata fileMetadata = minioService.getFileMetadata(fileId);

            FileMetadataResponse response = FileMetadataResponse.builder()
                    .id(fileMetadata.getId())
                    .originalFilename(fileMetadata.getOriginalFilename())
                    .fileSize(fileMetadata.getFileSize())
                    .contentType(fileMetadata.getContentType())
                    .uploadedAt(fileMetadata.getUploadedAt())
                    .build();

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileMetadataResponse>> listAllFiles() {
        try {
            List<FileMetadata> files = minioService.listAllFiles();
            List<FileMetadataResponse> response = files.stream()
                    .map(fm -> FileMetadataResponse.builder()
                            .id(fm.getId())
                            .originalFilename(fm.getOriginalFilename())
                            .fileSize(fm.getFileSize())
                            .contentType(fm.getContentType())
                            .uploadedAt(fm.getUploadedAt())
                            .build())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID fileId) {
        try {
            var metadata = minioService.getFileMetadata(fileId);
            var fileStream = minioService.downloadFile(fileId);

            String contentType = metadata.getContentType();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", metadata.getOriginalFilename());

            var resource = new InputStreamResource(fileStream);
            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<String> delete(@PathVariable UUID fileId) {
        try {
            minioService.deleteFile(fileId);
            return ResponseEntity.ok()
                    .body("File deleted successfully: " + fileId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("File not found: " + fileId);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting file: " + e.getMessage());
        }
    }
}
