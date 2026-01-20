package com.dropzone.api.service;

import com.dropzone.api.model.FileMetadata;
import com.dropzone.api.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileCleanupService {

    private final FileRepository fileRepository;
    private final Path rootLocation;

    public FileCleanupService(FileRepository fileRepository,
                              @Value("${app.storage.location}") String storageLocation) {
        this.fileRepository = fileRepository;
        this.rootLocation = Paths.get(storageLocation);
    }

    // Runs every 60 seconds (60000 ms)
    @Scheduled(fixedRate = 60000)
    @Transactional // Ensures DB delete and File delete happen together (mostly)
    public void cleanupExpiredFiles() {

        System.out.println("🧹 Janitor is working... Checking for expired files.");

        // 1. Find files that are PAST their expiry time
        List<FileMetadata> timeExpiredFiles = fileRepository.findByExpiryTimeBefore(LocalDateTime.now());
        List<FileMetadata> limitExpiredFiles = fileRepository.findByExpiryLimit();

        // 2. Loop through and delete them
        for (FileMetadata file : timeExpiredFiles) {
            deleteFile(file);
        }

        for (FileMetadata file : limitExpiredFiles) {
            deleteFile(file);
        }
    }

    private void deleteFile(FileMetadata file) {
        try {
            // A. Delete from Hard Drive
            Path filePath = rootLocation.resolve(file.getStorageName());
            Files.deleteIfExists(filePath);

            // B. Delete from Database
            fileRepository.delete(file);

            System.out.println("❌ Deleted expired file: " + file.getOriginalFilename());

        } catch (IOException e) {
            System.err.println("⚠️ Failed to delete file from disk: " + file.getStorageName());
        }
    }
}