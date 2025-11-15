# MinIO Demo

A simple, production-ready Spring Boot service for file storage using MinIO (AWS S3-compatible) and PostgreSQL for metadata. It lets you:

- Upload files
- Retrieve file metadata
- List all uploaded files
- Download files
- Delete files

Files are stored in a MinIO bucket; metadata (filename, size, content type, timestamps, etc.) is persisted in PostgreSQL via Spring Data JPA.


## Tools

- Java 17, Spring Boot 3
- PostgreSQL
- MinIO Java SDK

## Architecture

- REST API
- Storage: MinIO bucket
- Metadata: PostgreSQL table `file_metadata`

```
Client ── HTTP ──> Spring Boot API
                    ├─ MinIO (binary objects)
                    └─ PostgreSQL (metadata rows)
```

## Prerequisites

- JDK 17+
- Docker Desktop (or Docker Engine) and Docker Compose


## Quick start

1) Start dependencies (MinIO + PostgreSQL):

   - Ports exposed: MinIO API 9000, MinIO Console 9001, Postgres 5432

   Using Docker Compose from this repository root:

   ```powershell
   docker compose up -d
   ```

2) Create the MinIO bucket (if not already present):

   - Open MinIO Console: http://localhost:9001 (login: `minioadmin` / `minioadmin123` from `docker-compose.yml`)
   - Create a bucket named `uploads` (or adjust `minio.bucket` in `application.yml`)

3) Run the Spring Boot app:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   The API will start on http://localhost:8080.


## API reference

Base URL: `http://localhost:8080`

- POST `/files/upload`
  - Content-Type: `multipart/form-data`
  - Field: `file=@<path>`
  - 201 Created →
    ```json
    {
      "id": "6d9a2a5a-...",
      "originalFilename": "example.png",
      "storedFilename": "6d9a2a5a-....png",
      "fileSize": 12345,
      "contentType": "image/png",
      "uploadedAt": "2025-11-14T22:12:15.010218",
      "message": "File uploaded successfully"
    }
    ```

- GET `/files/{fileId}/metadata`
  - 200 OK →
    ```json
    {
      "id": "6d9a2a5a-...",
      "originalFilename": "example.png",
      "fileSize": 12345,
      "contentType": "image/png",
      "uploadedAt": "2025-11-14T22:12:15.010218"
    }
    ```
  - 404 if not found

- GET `/files/list`
  - 200 OK → array of the same shape as metadata above

- GET `/files/download/{fileId}`
  - 200 OK → file stream (Content-Type from original upload)
  - 404 if object missing

- DELETE `/files/delete/{fileId}`
  - 200 OK → plain text confirmation
  - 404 if not found
