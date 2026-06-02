package iuh.fit.uploadservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import iuh.fit.uploadservice.enums.MediaUploadPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaUploadResult {
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("key")
    private String key;
    
    @JsonProperty("fileName")
    private String fileName;
    
    @JsonProperty("fileSize")
    private long fileSize;
    
    @JsonProperty("mimeType")
    private String mimeType;
    
    @JsonProperty("purpose")
    private MediaUploadPurpose purpose;
    
    @JsonProperty("uploadedAt")
    private long uploadedAt;

}
