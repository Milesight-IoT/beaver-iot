package com.milesight.beaveriot.cluster;

import com.milesight.beaveriot.base.cluster.ClusterAware;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * @author leon
 */
public class DataSourceClusterValidator implements ClusterAware {

    private static final String[] UNSUPPORTED_DATASOURCE_URL_PREFIX = new String[]{"jdbc:h2:file:", "jdbc:h2:mem:"};
    private DataSource dataSource;

    public DataSourceClusterValidator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean isClusterSupported() {
        try {
            String url = dataSource.getConnection().getMetaData().getURL().toLowerCase();
            boolean match = Arrays.stream(UNSUPPORTED_DATASOURCE_URL_PREFIX).anyMatch(url::startsWith);
            if (match) {
                throw new IllegalStateException("Datasource url is not supported in cluster mode :" + url);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
