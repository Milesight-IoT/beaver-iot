package com.milesight.beaveriot.cluster.autoconfigure;

import com.milesight.beaveriot.cluster.ClusterProperties;
import com.milesight.beaveriot.cluster.ClusterValidationInitializingBean;
import com.milesight.beaveriot.cluster.DataSourceClusterValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * @author leon
 */
@EnableConfigurationProperties(ClusterProperties.class)
public class ClusterAutoConfiguration {

    @Bean
    public ClusterValidationInitializingBean clusterValidationInitializingBean(ClusterProperties clusterProperties) {
        return new ClusterValidationInitializingBean(clusterProperties);
    }

    @Bean
    public DataSourceClusterValidator dataSourceClusterValidator(DataSource dataSource) {
        return new DataSourceClusterValidator(dataSource);
    }

}
