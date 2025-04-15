package com.milesight.beaveriot.resource;

import com.milesight.beaveriot.resource.config.ResourceConstants;

/**
 * ResourceHelper class.
 *
 * @author simon
 * @date 2025/4/3
 */
public class ResourceHelper {
    private ResourceHelper() {}

    public static String getBucketPolicy(String brand, String bucketName) {
        return "{\n" +
                "  \"Version\":\"2012-10-17\",\n" +
                "  \"Statement\":[{\n" +
                "    \"Effect\":\"Allow\",\n" +
                "    \"Principal\":\"*\",\n" +
                "    \"Action\":[\"s3:GetObject\"],\n" +
                "    \"Resource\":\"arn:" + brand + ":s3:::" + bucketName + "/" + ResourceConstants.PUBLIC_PATH_PREFIX + "/*\"\n" +
                "  }]\n" +
                "}";
    }
}
