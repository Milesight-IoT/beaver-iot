package com.milesight.beaveriot.resource.adapter.s3;

import com.milesight.beaveriot.resource.adapter.BaseResourceAdapter;
import com.milesight.beaveriot.resource.config.ResourceConstants;
import com.milesight.beaveriot.resource.config.ResourceSettings;
import com.milesight.beaveriot.resource.model.PutResourceRequest;
import com.milesight.beaveriot.resource.model.ResourceStat;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * MinioResourceAdapter class.
 *
 * @author simon
 * @date 2025/4/3
 */
@Slf4j
public class S3ResourceAdapter implements BaseResourceAdapter {
    MinioClient s3Client;

    String bucketName;

    String endpoint;

    Duration preSignExpire;

    @SneakyThrows
    private void checkBucket() {
        BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder()
                .bucket(bucketName)
                .build();
        if (!s3Client.bucketExists(bucketExistsArgs)) {
            throw new MinioException("Bucket not found: " + bucketName);
        }
    }

    public S3ResourceAdapter(ResourceSettings settings) {
        this.bucketName = settings.getS3().getBucket();
        this.endpoint = settings.getS3().getEndpoint();
        this.preSignExpire = settings.getPreSignExpire();
        s3Client = MinioClient.builder()
                .credentials(settings.getS3().getAccessKey(), settings.getS3().getAccessSecret())
                .region(settings.getS3().getRegion())
                .endpoint(settings.getS3().getEndpoint())
                .build();
        this.checkBucket();
    }

    @Override
    @SneakyThrows
    public String generatePutResourcePreSign(String objKey) {
        GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(bucketName)
                .object(objKey)
                .expiry((int) preSignExpire.getSeconds(), TimeUnit.SECONDS)
                .build();
        return s3Client.getPresignedObjectUrl(getPresignedObjectUrlArgs);
    }

    @Override
    @SneakyThrows
    public String resolveResourceUrl(String objKey) {
        GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(objKey)
                .build();
        return s3Client.getPresignedObjectUrl(getPresignedObjectUrlArgs).split("\\?", 2)[0];
    }

    @Override
    public ResourceStat stat(String objKey) {
        StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                .bucket(bucketName)
                .object(objKey)
                .build();
        try {
            StatObjectResponse response = s3Client.statObject(statObjectArgs);
            ResourceStat stat = new ResourceStat();
            stat.setSize(response.size());
            stat.setContentType(response.contentType());
            return stat;
        } catch (Exception e) {
            log.info("Get obj " + objKey + " error: " + e.getMessage());
            return null;
        }
    }

    @Override
    @SneakyThrows
    public byte[] get(String objKey) {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objKey)
                .build();
        GetObjectResponse getObjectResponse = s3Client.getObject(getObjectArgs);
        return getObjectResponse.readAllBytes();
    }

    @Override
    @SneakyThrows
    public void delete(String objKey) {
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objKey)
                .build();
        s3Client.removeObject(removeObjectArgs);
    }

    @Override
    @SneakyThrows
    public void putResource(PutResourceRequest request) {
        PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(request.getObjectKey())
                .contentType(request.getContentType())
                .stream(request.getContentInput(), request.getContentLength(), ResourceConstants.MAX_FILE_SIZE)
                .build();
        s3Client.putObject(putObjectArgs);
    }
}
