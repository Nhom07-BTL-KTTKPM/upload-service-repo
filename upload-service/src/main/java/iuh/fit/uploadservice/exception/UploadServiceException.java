package iuh.fit.uploadservice.exception;

public class UploadServiceException extends RuntimeException {
    
    public UploadServiceException(String message) {
        super(message);
    }

    public UploadServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
