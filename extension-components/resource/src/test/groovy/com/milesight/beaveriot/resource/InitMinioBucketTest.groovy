package com.milesight.beaveriot.resource

import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.SetBucketPolicyArgs
import spock.lang.Specification


class InitMinioBucketTest extends Specification {
    private static final String endpoint = "http://192.168.43.48:9000/";
    private static final String region = "unknown";
    private static final String accessKey = "dlCGKICiYf6PMPkokXrq";
    private static final String accessSecret = "xdVi8mgN2RUZluXSOgCmsExrmvn2dJawYFaE4wmG";
    private static final String bucketName = "beaver-iot-resource";
    private static final minioClient = MinioClient.builder()
            .credentials(accessKey, accessSecret)
            .region(region)
            .endpoint(endpoint)
            .build();

    def "test create minio bucket"() {
        when:
        MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder()
                .bucket(bucketName)
                .build();
        minioClient.makeBucket(makeBucketArgs);

        SetBucketPolicyArgs setBucketPolicyArgs = SetBucketPolicyArgs.builder()
                .bucket(bucketName)
                .config(ResourceHelper.getBucketPolicy("aws", bucketName))
                .build();
        minioClient.setBucketPolicy(setBucketPolicyArgs);
        then:
        println("Bucket name:" + bucketName)
    }
}