package com.milesight.beaveriot.resource.adapter.awscn;

import com.milesight.beaveriot.resource.adapter.aws.AwsResourceAdapter;
import com.milesight.beaveriot.resource.config.ResourceSettings;

/**
 * AwsCnResourceAdapter class.
 *
 * @author simon
 * @date 2025/4/3
 */
public class AwsCnResourceAdapter extends AwsResourceAdapter {
    public AwsCnResourceAdapter(ResourceSettings settings) {
        super(settings);
    }

    @Override
    protected String getAwsBrand() {
        return "aws-cn";
    }
}
