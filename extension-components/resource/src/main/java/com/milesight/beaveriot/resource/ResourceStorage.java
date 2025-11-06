package com.milesight.beaveriot.resource;

import com.milesight.beaveriot.resource.adapter.BaseResourceAdapter;
import com.milesight.beaveriot.resource.config.ResourceConstants;
import com.milesight.beaveriot.resource.model.PreSignResult;
import com.milesight.beaveriot.resource.model.PutResourceRequest;
import com.milesight.beaveriot.resource.model.ResourceStat;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * AbstractResourceStorage class.
 *
 * @author simon
 * @date 2025/4/1
 */
@Component
@Slf4j
public class ResourceStorage {
    @Resource
    BaseResourceAdapter resourceAdapter;

    /**
     * Get an url that can be uploaded to.
     *
     * @return target url
     */
    public PreSignResult createUploadPreSign(String inputFilename) {
        String filename = inputFilename.trim();
        if (filename.length() > ResourceConstants.MAX_OBJECT_NAME_LENGTH || filename.matches(ResourceConstants.INVALID_OBJECT_NAME_CHARS)) {
            throw new IllegalArgumentException("Invalid file name: " + inputFilename);
        }

        String key = ResourceConstants.PUBLIC_PATH_PREFIX + "/" + UUID.randomUUID() + "-" + filename;
        String url = resourceAdapter.generatePutResourcePreSign(key);
        PreSignResult result = new PreSignResult();
        result.setUploadUrl(url);
        result.setResourceUrl(resourceAdapter.resolveResourceUrl(key));
        result.setKey(key);
        return result;
    }

    public ResourceStat stat(String key) {
        return resourceAdapter.stat(key);
    }

    /**
     * Upload resource
     */
    public void upload(String objKey, String contentType, byte[] data) {
        PutResourceRequest request = new PutResourceRequest();
        request.setObjectKey(objKey);
        request.setContentType(contentType);
        request.setContentInput(new ByteArrayInputStream(data));
        request.setContentLength((long) data.length);
        resourceAdapter.putResource(request);
    }

    /**
     * Get resource
     */
    public InputStream get(String objKey) {
        byte[] data = resourceAdapter.get(objKey);
        if (data == null) {
            return null;
        }

        return new ByteArrayInputStream(data);
    }

    /**
     * Fetch resource data
     */
    public String fetchData() {
        return null;
    }

    /**
     * Move the resource to permanent storage.
     */
    public void delete(String key) {
        resourceAdapter.delete(key);
    }
}
