package com.dropzone.api.controller;

import com.dropzone.api.model.FileMetadata;
import com.dropzone.api.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

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
            @RequestParam(value = "minutes", defaultValue = "10") int expiryMinutes,
            @RequestParam(value = "password", required = false) String password) throws IOException {

        String fileName = file.getOriginalFilename();
        assert fileName != null;
        if (fileName.endsWith(".exe") || fileName.endsWith(".bat") || fileName.endsWith(".sh")) {
            return ResponseEntity.badRequest().build();
        }

        // Call the service to save the file
        FileMetadata metadata = storageService.store(file, maxDownloads, expiryMinutes, password);

        // We return the full metadata object so the frontend sees the ID and expiry
        return ResponseEntity.ok(metadata);
    }

    // 2. Download Endpoint
    // GET http://localhost:8080/api/files/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id, @RequestParam(value = "password", required = false) String inputPassword) throws IOException {

        // 1. Check metadata (Expiry logic)
        FileMetadata metadata = storageService.getMetadata(id);

        if (metadata.isExpired()) {
            return ResponseEntity.status(410).body(null); // 410 GONE (Self-destructed)
        }

        if (metadata.getPassword() != null && !metadata.getPassword().isEmpty()) {
            // 3. CHECK: Did the user provide the CORRECT password?
            if (inputPassword == null || !inputPassword.equals(metadata.getPassword())) {
                // REJECT: Return 403 Forbidden
                return ResponseEntity.status(403).body(null);
            }
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

    // 2. All files view Endpoint
    // GET http://localhost:8080/api/files/admin/list
    @GetMapping("/admin/list")
    public List<FileMetadata> getAllFiles() {
        return storageService.getAllFiles();
    }

    //Debug endpoint
    // GET http://localhost:8080/api/files/{id}/debug
    @GetMapping("{id}/debug")
    public String Debug(@PathVariable String id) throws IOException {
        FileMetadata metadata = storageService.getMetadata(id);
        long originalSize = metadata.getSize();

        Path encryptedFilePath = Paths.get("./dropzone_uploads/" + metadata.getStorageName());
        long storedOnDiskSize = Files.size(encryptedFilePath);
        return String.format(
                "Experiment Result: User sent: %d bytes | Stored on disk: %d bytes (Difference: %d bytes)",
                originalSize,
                storedOnDiskSize,
                (storedOnDiskSize - originalSize)
        );
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verifyPassword(
            @PathVariable String id,
            @RequestParam(value = "password", required = false) String inputPassword) {

        // 1. Get Metadata directly (Do not use storageService.loadAsResource)
        FileMetadata metadata = storageService.getMetadata(id);
        // ^ Ensure you have a method in StorageService that just returns metadata/repo.findById

        if (metadata == null) return ResponseEntity.notFound().build();

        // 2. Check Expiry
        if (metadata.isExpired()) {
            return ResponseEntity.status(410).body("File Expired");
        }

        // 3. Check Password
        if (metadata.getPassword() != null && !metadata.getPassword().isEmpty()) {
            if (inputPassword == null || !inputPassword.equals(metadata.getPassword())) {
                return ResponseEntity.status(403).body("Incorrect Password");
            }
        }

        // 4. Return OK (We DO NOT increment the counter here)
        return ResponseEntity.ok().build();
    }

    @GetMapping("/getIp")
    private String getServerIp() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            return ip;
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }
}