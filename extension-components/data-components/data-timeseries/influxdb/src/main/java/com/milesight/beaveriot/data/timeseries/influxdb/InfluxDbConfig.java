package com.milesight.beaveriot.data.timeseries.influxdb;

import com.milesight.beaveriot.data.timeseries.common.TimeSeriesProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * InfluxDbConfig class.
 *
 * @author simon
 * @date 2025/10/14
 */
@Component
@Data
@ConditionalOnProperty(name = InfluxDbConstants.URL_CONFIG)
@ConfigurationProperties(prefix = InfluxDbConstants.CONFIG_PREFIX)
public class InfluxDbConfig {
    private String url;

    private String token;

    private String org;

    @Autowired
    private TimeSeriesProperty timeSeriesProperty;

    @Bean
    InfluxDbClient influxDbClient() {
        InfluxDbClient client = new InfluxDbClient(url, token, org);
        timeSeriesProperty.getRetention().forEach(client::ensureBucket);
        return client;
    }
}
