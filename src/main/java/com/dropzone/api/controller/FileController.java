package com.dropzone.api.controller;

import com.dropzone.api.model.FileMetadata;
import com.dropzone.api.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
// @CrossOrigin(origins = "*") // Uncomment if you build a separate Frontend (React/Vue) later
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    // 1. Upload Endpoint
    // POST http://localhost:8080/api/files/upload
    // Body: form-data key="file", key="minutes" (optional)
    @PostMapping("/upload")
    public ResponseEntity<FileMetadata> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "downloads", defaultValue = "1") int maxDownloads,
            @RequestParam(value = "minutes", defaultValue = "10") int expiryMinutes) throws IOException {

        // Call the service to save the file
        FileMetadata metadata = storageService.store(file, maxDownloads, expiryMinutes);

        // We return the full metadata object so the frontend sees the ID and expiry
        return ResponseEntity.ok(metadata);
    }

    // 2. Download Endpoint
    // GET http://localhost:8080/api/files/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id) throws IOException {

        // 1. Check metadata (Expiry logic)
        FileMetadata metadata = storageService.getMetadata(id);

        if (metadata.isExpired()) {
            return ResponseEntity.status(410).body(null); // 410 GONE (Self-destructed)
        }

        // 2. Load the actual resource
        Resource resource = storageService.loadAsResource(id);

        // 3. Increment download count (The "Self-Destruct" counter)
        // Note: In a real app, this should be atomic/synchronized to prevent race conditions
        // but for now, we just call a service method to update DB.
         storageService.incrementDownloadCount(id); // (We need to add this method!)

        // 4. Return the file stream with correct headers
        return ResponseEntity.ok()
                // "attachment" forces the browser to download instead of opening
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, metadata.getContentType())
                .body(resource);
    }
}