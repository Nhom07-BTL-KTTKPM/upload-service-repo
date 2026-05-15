package iuh.fit.uploadservice.service;

import iuh.fit.uploadservice.dto.MediaUploadResult;
import iuh.fit.uploadservice.enums.MediaUploadPurpose;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaUploadService {

    /**
     * Upload single file to S3
     * @param file MultipartFile to upload
     * @param purpose Upload purpose (AVATAR, POST, COMMENT, PRODUCT)
     * @return MediaUploadResult with S3 URL and metadata
     */
    MediaUploadResult uploadFile(MultipartFile file, MediaUploadPurpose purpose);

    /**
     * Upload multiple files to S3
     * @param files List of MultipartFile to upload
     * @param purpose Upload purpose
     * @return List of MediaUploadResult
     */
    List<MediaUploadResult> uploadMultiple(List<MultipartFile> files, MediaUploadPurpose purpose);

    /**
     * Delete file from S3
     * @param key S3 object key
     */
    void deleteFile(String key);

    /**
     * Get public URL for uploaded file
     * @param key S3 object key
     * @return Public URL
     */
    String getPublicUrl(String key);
}
