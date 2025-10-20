package com.milesight.beaveriot.data.timeseries.influxdb;

import com.influxdb.client.*;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.BucketRetentionRules;
import com.influxdb.client.domain.Organization;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.time.Duration;

/**
 * InfluxDbClient class.
 *
 * @author simon
 * @date 2025/10/11
 */
public class InfluxDbClient implements DisposableBean {
    private final InfluxDBClient client;
    @Getter
    private final String orgName;
    private Organization organization = null;
    public InfluxDbClient(String url, String token, String orgName) {
        client = InfluxDBClientFactory.create(url, token.toCharArray(), orgName);
        this.orgName = orgName;
        for (Organization org : client.getOrganizationsApi().findOrganizations()) {
            if (org.getName().equals(orgName)) {
                this.organization = org;
                break;
            }
        }

        if (this.organization == null) {
            throw new IllegalArgumentException("Cannot find organization: " + orgName);
        }
    }

    public BucketsApi getBucketsApi() {
        return client.getBucketsApi();
    }

    public void ensureBucket(String name, Duration retention) {
        BucketsApi bucketsApi = getBucketsApi();

        Bucket bucket = bucketsApi.findBucketByName(name);
        if (bucket != null) {
            return;
        }

        if (retention == null || retention.isZero() || retention.isNegative()) {
            bucketsApi.createBucket(name, this.organization);
            return;
        }

        BucketRetentionRules retentionRules = new BucketRetentionRules();
        retentionRules.setEverySeconds((int) retention.getSeconds());

        bucketsApi.createBucket(name, retentionRules, this.organization);
    }

    public QueryApi getQueryApi() {
        return client.getQueryApi();
    }

    public WriteApi getWriteApi() {
        return client.makeWriteApi();
    }

    @Override
    public void destroy() throws Exception {
        client.close();
    }
}
