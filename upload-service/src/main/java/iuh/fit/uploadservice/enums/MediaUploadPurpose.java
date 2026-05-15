package iuh.fit.uploadservice.enums;

public enum MediaUploadPurpose {
    AVATAR("avatars"),
    COMMENT("comments"),
    PRODUCT("products");

    private final String prefix;

    MediaUploadPurpose(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public static MediaUploadPurpose fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Upload purpose cannot be empty");
        }
        try {
            return MediaUploadPurpose.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid upload purpose: " + value);
        }
    }
}
