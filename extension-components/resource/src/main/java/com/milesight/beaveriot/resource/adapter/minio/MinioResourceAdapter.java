package com.milesight.beaveriot.resource.adapter.minio;

import com.milesight.beaveriot.resource.adapter.BaseResourceAdapter;
import com.milesight.beaveriot.resource.config.ResourceConstants;
import com.milesight.beaveriot.resource.config.ResourceHelper;
import com.milesight.beaveriot.resource.config.ResourceSettings;
import com.milesight.beaveriot.resource.model.PutResourceRequest;
import com.milesight.beaveriot.resource.model.ResourceStat;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * MinioResourceAdapter class.
 *
 * @author simon
 * @date 2025/4/3
 */
@Slf4j
public class MinioResourceAdapter implements BaseResourceAdapter {
    MinioClient minioClient;

    String bucketName;

    String endpoint;

    @SneakyThrows
    private void initBucket() {
        MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder()
                .bucket(bucketName)
                .build();
        minioClient.makeBucket(makeBucketArgs);

        SetBucketPolicyArgs setBucketPolicyArgs = SetBucketPolicyArgs.builder()
                .bucket(bucketName)
                .config(ResourceHelper.getBucketPolicy(bucketName))
                .build();
        minioClient.setBucketPolicy(setBucketPolicyArgs);
    }

    @SneakyThrows
    private void checkBucket() {
        BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder()
                .bucket(bucketName)
                .build();
        if (!minioClient.bucketExists(bucketExistsArgs)) {
            throw new MinioException("Bucket not found: " + bucketName);
        }
    }

    public MinioResourceAdapter(ResourceSettings settings) {
        this.bucketName = settings.getS3().getBucket();
        this.endpoint = settings.getS3().getEndpoint();
        minioClient = MinioClient.builder()
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
                .expiry(ResourceConstants.PUT_RESOURCE_PRE_SIGN_EXPIRY_MINUTES, TimeUnit.MINUTES)
                .build();
        return minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
    }

    @Override
    @SneakyThrows
    public String resolveResourceUrl(String objKey) {
        GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(objKey)
                .build();
        return minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs).split("\\?", 2)[0];
    }

    @Override
    public ResourceStat stat(String objKey) {
        StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                .bucket(bucketName)
                .object(objKey)
                .build();
        try {
            StatObjectResponse response = minioClient.statObject(statObjectArgs);
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
        GetObjectResponse getObjectResponse = minioClient.getObject(getObjectArgs);
        return getObjectResponse.readAllBytes();
    }

    @Override
    @SneakyThrows
    public void delete(String objKey) {
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objKey)
                .build();
        minioClient.removeObject(removeObjectArgs);
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
        minioClient.putObject(putObjectArgs);
    }
}
