package iuh.fit.uploadservice.service.impl;

import iuh.fit.uploadservice.dto.MediaUploadResult;
import iuh.fit.uploadservice.enums.MediaUploadPurpose;
import iuh.fit.uploadservice.exception.UploadServiceException;
import iuh.fit.uploadservice.service.MediaUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MediaUploadServiceImpl implements MediaUploadService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicBaseUrl;
    private final Map<MediaUploadPurpose, String> prefixes;

    // Allowed MIME types
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime"
    );

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    public MediaUploadServiceImpl(
            S3Client s3Client,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.public-base-url}") String publicBaseUrl,
            @Value("${upload.s3.prefixes.avatar}") String avatarPrefix,
            @Value("${upload.s3.prefixes.comment}") String commentPrefix,
            @Value("${upload.s3.prefixes.product}") String productPrefix
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.publicBaseUrl = publicBaseUrl;
        
        this.prefixes = new HashMap<>();
        this.prefixes.put(MediaUploadPurpose.AVATAR, avatarPrefix);
        this.prefixes.put(MediaUploadPurpose.COMMENT, commentPrefix);
        this.prefixes.put(MediaUploadPurpose.PRODUCT, productPrefix);
    }

    @Override
    public MediaUploadResult uploadFile(MultipartFile file, MediaUploadPurpose purpose) {
        validateFile(file);
        
        try {
            String key = generateS3Key(file.getOriginalFilename(), purpose);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromBytes(file.getBytes())
            );

            String url = getPublicUrl(key);
            
            log.info("File uploaded successfully. Key: {}, Purpose: {}", key, purpose);

            return MediaUploadResult.builder()
                    .url(url)
                    .key(key)
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .purpose(purpose)
                    .uploadedAt(System.currentTimeMillis())
                    .build();
        } catch (IOException e) {
            log.error("Error reading file: {}", file.getOriginalFilename(), e);
            throw new UploadServiceException("Failed to read file", e);
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", file.getOriginalFilename(), e);
            throw new UploadServiceException("Failed to upload file to S3", e);
        }
    }

    @Override
    public List<MediaUploadResult> uploadMultiple(List<MultipartFile> files, MediaUploadPurpose purpose) {
        // Avatar only allows single upload
        if (purpose == MediaUploadPurpose.AVATAR) {
            throw new UploadServiceException("Avatar does not support multiple file uploads. Please use single file upload.");
        }
        
        return files.stream()
                .map(file -> uploadFile(file, purpose))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully. Key: {}", key);
        } catch (Exception e) {
            log.error("Error deleting file from S3. Key: {}", key, e);
            throw new UploadServiceException("Failed to delete file from S3", e);
        }
    }

    @Override
    public String getPublicUrl(String key) {
        if (publicBaseUrl != null && !publicBaseUrl.isEmpty()) {
            return publicBaseUrl + "/" + key;
        }
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UploadServiceException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new UploadServiceException("File size exceeds maximum allowed size of 100MB");
        }

        String contentType = file.getContentType();
        if (!isAllowedMimeType(contentType)) {
            throw new UploadServiceException("File type not allowed. Allowed types: images and videos");
        }
    }

    private boolean isAllowedMimeType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return ALLOWED_IMAGE_TYPES.contains(contentType) || ALLOWED_VIDEO_TYPES.contains(contentType);
    }

    private String generateS3Key(String originalFilename, MediaUploadPurpose purpose) {
        String prefix = prefixes.get(purpose);
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID() + fileExtension;
        return prefix + "/" + uniqueFileName;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
