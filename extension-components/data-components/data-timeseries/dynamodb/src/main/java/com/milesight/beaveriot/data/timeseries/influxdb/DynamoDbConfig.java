package com.milesight.beaveriot.data.timeseries.influxdb;

import com.milesight.beaveriot.data.timeseries.common.TimeSeriesProperty;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * author: Luxb
 * create: 2025/11/3 8:56
 **/
@Component
@Data
@ConditionalOnProperty(name = DynamoDbConstants.ENDPOINT_CONFIG)
@ConfigurationProperties(prefix = DynamoDbConstants.CONFIG_PREFIX)
public class DynamoDbConfig {
    private String endpoint;
    private String region;
    private String accessId;
    private String accessKey;

    @Resource
    private TimeSeriesProperty timeSeriesProperty;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .httpClient(UrlConnectionHttpClient.builder().build())
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessId, accessKey)
                ))
                .build();
    }
}