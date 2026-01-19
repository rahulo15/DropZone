package com.dropzone.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.nio.file.Path;
import java.time.LocalDateTime;

@Entity
@Table(name = "files") // Renaming table to 'files' for clarity
@Data // Generates Getters, Setters, ToString, EqualsAndHashCode
@Builder // Allows us to create objects cleanly
@NoArgsConstructor // Required by JPA
@AllArgsConstructor
public class FileMetadata {

    @Id
    private String id; // The Short Code (e.g., "xh52ka") - This is the Primary Key

    private String originalFilename; // "resume.pdf"

    // The UUID name on disk (e.g., "550e8400-e29b...").
    // This prevents "resume.pdf" from overwriting another "resume.pdf"
    private String storageName;

    private long size;
    private String contentType; // "application/pdf"

    // --- Self-Destruct Rules ---
    private int maxDownloads;
    private int downloadCount;

    private LocalDateTime uploadTime;
    private LocalDateTime expiryTime;

    // Helper method to check if file is dead
    public boolean isExpired() {
        return (downloadCount >= maxDownloads) || LocalDateTime.now().isAfter(expiryTime);
    }
}