package com.mohamed.miniodemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private UUID id;
    private String originalFilename;
    private String storedFilename;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
    private String message;
}