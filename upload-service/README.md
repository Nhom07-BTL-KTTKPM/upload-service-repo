# Upload Service Configuration Guide

## Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 12+ (for metadata storage)
- AWS S3 Account with credentials

## AWS S3 Configuration

### Step 1: Create S3 Bucket
1. Go to AWS Console → S3
2. Create bucket: `cosmetics-upload-{environment}`
3. Enable versioning (optional)
4. Block public access (files accessed via signed URLs)

### Step 2: Create IAM User
1. Go to IAM Console
2. Create user with S3 access policy:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::your-bucket-name/*",
        "arn:aws:s3:::your-bucket-name"
      ]
    }
  ]
}
```
3. Generate Access Key ID and Secret Access Key

### Step 3: Configure .env
```
AWS_S3_BUCKET_NAME=cosmetics-upload-dev
AWS_REGION=ap-southeast-1
AWS_ACCESS_KEY_ID=your-access-key-id
AWS_SECRET_ACCESS_KEY=your-secret-access-key
AWS_S3_PUBLIC_BASE_URL=https://cosmetics-upload-dev.s3.ap-southeast-1.amazonaws.com
```

## Upload Purposes

The service supports 3 upload purposes with different prefixes:

1. **AVATAR** - User profile pictures
   - Path: `avatars/{uuid}.{ext}`
   - Max size: 100MB
   - Allowed: Images only

2. **COMMENT** - Comment attachments
   - Path: `comments/{uuid}.{ext}`
   - Max size: 100MB
   - Allowed: Images & Videos

3. **PRODUCT** - Product images
   - Path: `products/{uuid}.{ext}`
   - Max size: 100MB
   - Allowed: Images & Videos

## API Endpoints

### Upload Single File
```
POST /api/v1/upload/single
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile
- purpose: AVATAR | COMMENT | PRODUCT
```

### Upload Multiple Files
```
POST /api/v1/upload/multiple
Content-Type: multipart/form-data

Parameters:
- files: List<MultipartFile>
- purpose: AVATAR | COMMENT | PRODUCT
```

### Delete File
```
DELETE /api/v1/upload/{key}
```

### Get Public URL
```
GET /api/v1/upload/url/{key}
```

### Health Check
```
GET /api/v1/upload/health
```

## Running Locally

```bash
# Install dependencies
mvn clean install

# Run application
mvn spring-boot:run
```

The service will:
1. Register with Eureka Discovery Service
2. Be accessible via API Gateway at `http://localhost:8080/api/v1/upload/**`
3. Direct access at `http://localhost:8008`

## Database Schema

Create `upload_db` in PostgreSQL with:

```sql
CREATE DATABASE upload_db;

-- Optional: Track uploaded files
CREATE TABLE uploaded_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    s3_key VARCHAR(255) NOT NULL,
    file_name VARCHAR(255),
    file_size BIGINT,
    mime_type VARCHAR(50),
    purpose VARCHAR(50),
    url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_user_id ON uploaded_files(user_id);
CREATE INDEX idx_s3_key ON uploaded_files(s3_key);
CREATE INDEX idx_purpose ON uploaded_files(purpose);
```

## Integration with Other Services

### Avatar Upload (User Service)
```
POST http://localhost:8080/api/v1/upload/single
?purpose=AVATAR
```

### Comment Attachment (Comment Service)
```
POST http://localhost:8080/api/v1/upload/multiple
?purpose=COMMENT
```

### Product Image Upload (Catalog Service)
```
POST http://localhost:8080/api/v1/upload/single
?purpose=PRODUCT
```

## Security Notes

- All endpoints except `/health` require JWT authentication
- Files are stored with unique UUIDs to prevent collisions
- Original filenames are not preserved (security)
- MIME type validation prevents malicious uploads
- File size limits prevent abuse (100MB max)
