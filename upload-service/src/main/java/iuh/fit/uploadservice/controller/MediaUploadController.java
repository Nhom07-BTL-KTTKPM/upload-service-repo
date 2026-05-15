package iuh.fit.uploadservice.controller;

import iuh.fit.uploadservice.dto.MediaUploadResult;
import iuh.fit.uploadservice.enums.MediaUploadPurpose;
import iuh.fit.uploadservice.service.MediaUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@Slf4j
public class MediaUploadController {

    private final MediaUploadService mediaUploadService;

    public MediaUploadController(MediaUploadService mediaUploadService) {
        this.mediaUploadService = mediaUploadService;
    }

    /**
     * Upload single file
     * @param file File to upload
     * @param purpose Upload purpose (AVATAR, POST, COMMENT, PRODUCT)
     * @return MediaUploadResult with S3 URL
     */
    @PostMapping("/single")
    public ResponseEntity<MediaUploadResult> uploadSingle(
            @RequestParam("file") MultipartFile file,
            @RequestParam("purpose") String purpose
    ) {
        log.info("Uploading single file. Purpose: {}, FileName: {}", purpose, file.getOriginalFilename());
        
        MediaUploadPurpose uploadPurpose = MediaUploadPurpose.fromString(purpose);
        MediaUploadResult result = mediaUploadService.uploadFile(file, uploadPurpose);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Upload multiple files
     * @param files Files to upload
     * @param purpose Upload purpose
     * @return List of MediaUploadResult
     */
    @PostMapping("/multiple")
    public ResponseEntity<?> uploadMultiple(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("purpose") String purpose
    ) {
        log.info("Uploading {} files. Purpose: {}", files.size(), purpose);
        
        MediaUploadPurpose uploadPurpose = MediaUploadPurpose.fromString(purpose);
        
        // Validate that avatar doesn't use multiple upload
        if (uploadPurpose == MediaUploadPurpose.AVATAR) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Avatar does not support multiple file uploads. Please use single file upload.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        List<MediaUploadResult> results = mediaUploadService.uploadMultiple(files, uploadPurpose);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    /**
     * Delete file from S3
     * @param key S3 object key
     * @return Success message
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<String> deleteFile(@PathVariable String key) {
        log.info("Deleting file. Key: {}", key);
        
        mediaUploadService.deleteFile(key);
        
        return ResponseEntity.ok("File deleted successfully");
    }

    /**
     * Get public URL for file
     * @param key S3 object key
     * @return Public URL
     */
    @GetMapping("/url/{key}")
    public ResponseEntity<String> getPublicUrl(@PathVariable String key) {
        String url = mediaUploadService.getPublicUrl(key);
        return ResponseEntity.ok(url);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Upload service is running");
    }
}
