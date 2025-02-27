package com.milesight.beaveriot.cluster;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author leon
 */
@Data
@ConfigurationProperties(prefix = "cluster")
public class ClusterProperties {

    private boolean enabled;

}
