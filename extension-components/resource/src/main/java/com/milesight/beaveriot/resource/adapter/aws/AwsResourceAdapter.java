package com.milesight.beaveriot.resource.adapter.aws;

import com.milesight.beaveriot.resource.adapter.BaseResourceAdapter;
import com.milesight.beaveriot.resource.config.ResourceConstants;
import com.milesight.beaveriot.resource.config.ResourceHelper;
import com.milesight.beaveriot.resource.config.ResourceSettings;
import com.milesight.beaveriot.resource.model.PutResourceRequest;
import com.milesight.beaveriot.resource.model.ResourceStat;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URI;
import java.time.Duration;

/**
 * AwsResourceAdapter class.
 *
 * @author simon
 * @date 2025/4/2
 */
@Slf4j
public class AwsResourceAdapter implements BaseResourceAdapter {
    S3Client s3Client;

    S3Presigner s3Presigner;

    String bucketName;

    String endpoint;

    public AwsResourceAdapter(ResourceSettings settings) {
        this.bucketName = settings.getS3().getBucket();
        this.endpoint = settings.getS3().getEndpoint();
        Region region = Region.of(settings.getS3().getRegion());
        StaticCredentialsProvider staticCredentialsProvider = StaticCredentialsProvider
                .create(AwsBasicCredentials.create(settings.getS3().getAccessKey(), settings.getS3().getAccessSecret()));
        s3Client = S3Client.builder()
                .endpointOverride(URI.create(settings.getS3().getEndpoint()))
                .region(region)
                .credentialsProvider(staticCredentialsProvider)
                .build();
        s3Presigner = S3Presigner.builder()
                .region(region)
                .credentialsProvider(staticCredentialsProvider)
                .build();
        try {
            this.checkBucket();
        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                log.error("Initialize s3 bucket error. Please ensure 's3:*' is in the Action list of the accessKey's policy.");
            } else {
                throw e;
            }
        }
    }

    protected String getAwsBrand() {
        return "aws";
    }
    
    private void checkBucket() {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            s3Client.headBucket(headBucketRequest);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw S3Exception.builder().message("Cannot access bucket: " + bucketName).build();
            } else {
                throw e;
            }
        }
    }

    private void initBucket() {
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build();
        s3Client.createBucket(createBucketRequest);
        this.initAccessBlock();
        this.initPolicy();
        this.initCors();
    }

    public void initAccessBlock() {
        PutPublicAccessBlockRequest putPublicAccessBlockRequest = PutPublicAccessBlockRequest.builder()
                .bucket(bucketName)
                .publicAccessBlockConfiguration(builder -> builder
                        .blockPublicAcls(false)
                        .ignorePublicAcls(false)
                        .blockPublicPolicy(false)
                        .restrictPublicBuckets(false))
                .build();
        s3Client.putPublicAccessBlock(putPublicAccessBlockRequest);
    }

    public void initPolicy() {
        PutBucketPolicyRequest putBucketPolicyRequest = PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(ResourceHelper.getBucketPolicy(getAwsBrand()))
                .build();
        s3Client.putBucketPolicy(putBucketPolicyRequest);
    }

    public void initCors() {
        CORSRule corsRule = CORSRule.builder()
                .allowedMethods("PUT")
                .allowedOrigins("*")
                .allowedHeaders("*")
                .build();
        PutBucketCorsRequest putBucketCorsRequest = PutBucketCorsRequest.builder()
                .corsConfiguration(builder -> builder.corsRules(corsRule))
                .bucket(bucketName)
                .build();
        s3Client.putBucketCors(putBucketCorsRequest);
    }

    @Override
    @SneakyThrows
    public String generatePutResourcePreSign(String objKey) {
        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner
                .presignPutObject(p -> p
                        .signatureDuration(Duration.ofMinutes(ResourceConstants.PUT_RESOURCE_PRE_SIGN_EXPIRY_MINUTES))
                        .putObjectRequest(builder -> builder
                            .bucket(bucketName)
                            .key(objKey)
                        )
                );

        return presignedPutObjectRequest.url().toString();
    }

    @Override
    @SneakyThrows
    public ResourceStat stat(String objKey) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objKey)
                .build();
        try {
            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            ResourceStat stat = new ResourceStat();
            stat.setContentType(response.contentType());
            stat.setSize(response.contentLength());
            return stat;
        } catch (Exception e) {
            log.info("Get obj " + objKey + " error: " + e.getMessage());
            return null;
        }
    }

    @Override
    @SneakyThrows
    public byte[] get(String objKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objKey)
                .build();
        return s3Client.getObject(getObjectRequest).readAllBytes();
    }

    @Override
    public void delete(String objKey) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objKey)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    @Override
    public void putResource(PutResourceRequest request) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(request.getObjectKey())
                .contentType(request.getContentType())
                .contentLength(request.getContentLength())
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(request.getContentInput(), request.getContentLength()));
    }

    @Override
    @SneakyThrows
    public String resolveResourceUrl(String objKey) {
        GetUrlRequest request = GetUrlRequest.builder()
                .bucket(bucketName)
                .key(objKey)
                .build();
        return s3Client.utilities().getUrl(request).toString();
    }
}
