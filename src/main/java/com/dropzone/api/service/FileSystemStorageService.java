package com.dropzone.api.service;

import com.dropzone.api.model.FileMetadata;
import com.dropzone.api.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service // Tells Spring: "This is a Service Bean. Create one on startup."
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation; // The folder path: ./dropzone_uploads
    private final FileRepository fileRepository; // Our connection to DB
    private final CipherService cipherService;

    // Constructor Injection (Best Practice)
    // Spring reads 'app.storage.location' from application.properties and injects it here
    public FileSystemStorageService(@Value("${app.storage.location}") String storageLocation,
                                    FileRepository fileRepository,
                                    CipherService cipherService) {
        this.rootLocation = Paths.get(storageLocation);
        this.fileRepository = fileRepository;
        this.cipherService = cipherService;
        init(); // Ensure the folder exists
    }

    private void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public FileMetadata store(MultipartFile file, int maxDownloads, int expiryMinutes) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }

        // 1. Generate the Short ID (e.g., "xh52ka") used for the URL
        String shortId = generateShortId(6);

        // 2. Generate the Storage Name (UUID) to prevent overwriting files with same name
        String storageName = UUID.randomUUID().toString();

        // 3. Prepare the path: ./dropzone_uploads/550e8400-e29b...
        Path destinationFile = this.rootLocation.resolve(Paths.get(storageName))
                .normalize().toAbsolutePath();

        // --- ENCRYPTION LOGIC START ---

        // 1. Generate a unique key for this file
        javax.crypto.SecretKey key = cipherService.generateKey();

        // 2. Stream: Input (Browser) -> Encryptor -> Output (File)
        try (java.io.InputStream inputStream = file.getInputStream();
             java.io.OutputStream outputStream = Files.newOutputStream(destinationFile); // Open file for writing
             java.io.OutputStream encryptedStream = cipherService.encryptStream(outputStream, key)) { // Wrap it

            // Manual Copy: Read from input, write to encrypted stream
            inputStream.transferTo(encryptedStream);
        }

        // --- ENCRYPTION LOGIC END ---

        // 5. Create the Metadata Object
        FileMetadata metadata = FileMetadata.builder()
                .id(shortId)
                .originalFilename(file.getOriginalFilename())
                .storageName(storageName)
                .size(file.getSize())
                .contentType(file.getContentType())
                .maxDownloads(maxDownloads)
                .downloadCount(0)
                .uploadTime(LocalDateTime.now())
                .expiryTime(LocalDateTime.now().plusMinutes(expiryMinutes))
                .encryptionKey(cipherService.keyToString(key))
                .build();

        // 6. Save Metadata to SQLite via Repository
        return fileRepository.save(metadata);
    }

    @Override
    public Resource loadAsResource(String id) throws IOException {
        // Find DB record first
        FileMetadata metadata = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found in DB: " + id));

        // Find file on disk using the UUID storageName, NOT the original filename
        Path filePath = rootLocation.resolve(metadata.getStorageName());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found on disk: " + id);
        }

        // 1. Retrieve the Key
        javax.crypto.SecretKey key = cipherService.stringToKey(metadata.getEncryptionKey());

        // 2. Open the file from disk
        java.io.InputStream fileStream = Files.newInputStream(filePath);

        // 3. Wrap it in the Decryptor
        java.io.InputStream decryptedStream = cipherService.decryptStream(fileStream, key);

        // 4. Return as a Resource
        // InputStreamResource tells Spring: "Don't look for a file path, just read this stream I'm giving you."
        return new InputStreamResource(decryptedStream);
    }

    @Override
    public FileMetadata getMetadata(String id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Metadata not found for id: " + id));
    }

    // --- Helper: Generates a random 6-char string (e.g. "aB3xZ9") ---
    private String generateShortId(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public void incrementDownloadCount(String id) {
        // 1. Fetch the metadata
        FileMetadata metadata = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Metadata not found for id: " + id));

        // 2. Increment the counter
        metadata.setDownloadCount(metadata.getDownloadCount() + 1);

        // 3. Save back to DB
        fileRepository.save(metadata);
    }

    @Override
    public List<FileMetadata> getAllFiles() {
        return fileRepository.findAll();
    }
}