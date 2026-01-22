package com.dropzone.api.service;

import com.dropzone.api.model.FileMetadata;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface StorageService {

    // 1. Save the file and return the Metadata (so the controller can give the user the URL)
    FileMetadata store(MultipartFile file, int maxDownloads, int expiryMinutes, String password) throws IOException;

    // 2. Load the actual file bytes for downloading
    Resource loadAsResource(String id) throws IOException;

    // 3. Just get the info (for checking expiry before download)
    FileMetadata getMetadata(String id);

    void incrementDownloadCount(String id);

    List<FileMetadata> getAllFiles();
}