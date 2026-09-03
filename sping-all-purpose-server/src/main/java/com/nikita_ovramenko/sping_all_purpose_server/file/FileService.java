package com.nikita_ovramenko.sping_all_purpose_server.file;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    @Value("${spring.app.aws_bucket_name}")
    private String bucketName;

    private final S3Presigner s3Presigner;

    public FileService(S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }

    /* Create a presigned URL to use in a subsequent PUT request */
    public String createPresignedUrl(String keyName, Map<String, String> metadata) {

        try {

            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .metadata(metadata)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(60)) // The URL expires in 60 seconds.
                    .putObjectRequest(objectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

            return presignedRequest.url().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create presigned PUT URL for key=" + keyName, e);
        }
    }

    public String createPresignedGetLink(String key) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest.builder().bucket(bucketName).key(key).build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60)) // The URL expires in 60 minutes.
                    .getObjectRequest(objectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

            return presignedRequest.url().toString();

        } catch (Exception e) {
            // Do not fail the caller (an email send) over one unreachable object, but never
            // return a blank string silently -- that renders as "images: [, , ]" in the mail.
            log.error("Failed to create presigned GET URL for key={}", key, e);
            return "<link unavailable for " + key + ">";
        }
    }

}
