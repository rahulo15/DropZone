package com.dropzone.api.repository;

import com.dropzone.api.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, String> {

    // We will use this later for the background cleaner task
    List<FileMetadata> findByExpiryTimeBefore(LocalDateTime now);

    @Query("""
        SELECT f FROM FileMetadata f
        WHERE f.downloadCount >= f.maxDownloads
    """)
    List<FileMetadata> findByExpiryLimit();
}