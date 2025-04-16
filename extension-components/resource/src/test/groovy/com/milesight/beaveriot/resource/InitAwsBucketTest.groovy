package com.milesight.beaveriot.resource


import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import spock.lang.Specification

/**
 * InitBucketTest class.
 *
 * @author simon
 * @date 2025/4/15
 */
class InitAwsBucketTest extends Specification {
    private static final String endpoint = "";
    private static final Region region = Region.US_EAST_1;
    private static final String accessKey = "";
    private static final String accessSecret = "";
    private static final String bucketName = "beaver-iot-resource";
    private static final StaticCredentialsProvider credentialProvider = StaticCredentialsProvider
            .create(AwsBasicCredentials.create(accessKey, accessSecret));

    private static final S3Client s3Client = S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(region)
            .credentialsProvider(credentialProvider)
            .build();

    def "test create aws bucket"() {
        when:
        initBucket();
        initAccessBlock();
        initPolicy();
        initCors();
        then:
        println("Bucket name:" + bucketName)
    }

    private static void initBucket() {
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build() as CreateBucketRequest;
        s3Client.createBucket(createBucketRequest);
    }

    private static void initAccessBlock() {
        PutPublicAccessBlockRequest putPublicAccessBlockRequest = PutPublicAccessBlockRequest.builder()
                .bucket(bucketName)
                .publicAccessBlockConfiguration(builder -> builder
                        .blockPublicAcls(false)
                        .ignorePublicAcls(false)
                        .blockPublicPolicy(false)
                        .restrictPublicBuckets(false))
                .build() as PutPublicAccessBlockRequest;
        s3Client.putPublicAccessBlock(putPublicAccessBlockRequest);
    }

    private static void initPolicy() {
        PutBucketPolicyRequest putBucketPolicyRequest = PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(ResourceHelper.getBucketPolicy(region.id().startsWith("cn-") ? "aws-cn" : "aws", bucketName))
                .build() as PutBucketPolicyRequest;
        s3Client.putBucketPolicy(putBucketPolicyRequest);
    }

    private static void initCors() {
        CORSRule corsRule = CORSRule.builder()
                .allowedMethods("PUT")
                .allowedOrigins("*")
                .allowedHeaders("*")
                .build();
        PutBucketCorsRequest putBucketCorsRequest = PutBucketCorsRequest.builder()
                .corsConfiguration(builder -> builder.corsRules(corsRule))
                .bucket(bucketName)
                .build() as PutBucketCorsRequest;
        s3Client.putBucketCors(putBucketCorsRequest);
    }
}
