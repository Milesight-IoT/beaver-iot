package com.milesight.beaveriot.devicetemplate.parser;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

/**
 * author: Luxb
 * create: 2025/5/15 10:26
 **/
@Service
public class CommonDeviceTemplateParser {
    protected static final String REQUIRED_KEY_TEMPLATE_TYPE = "template_type";
    protected static final String REQUIRED_KEY_DEFINITION = "definition";
    private static final Set<String> REQUIRED_KEYS = Set.of(REQUIRED_KEY_TEMPLATE_TYPE, REQUIRED_KEY_DEFINITION);

    public String getTemplateType(String deviceTemplateContent) {
        Yaml yaml = new Yaml();
        try {
            Map<String, Object> templateMap = yaml.load(deviceTemplateContent);
            // Check required keys
            checkRequiredKeys(templateMap);
            return templateMap.get(REQUIRED_KEY_TEMPLATE_TYPE).toString().toUpperCase();
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "YAML syntax error:" + e.getMessage()).build();
        }
    }

    private void checkRequiredKeys(Map<String, Object> templateMap) {
        if (CollectionUtils.isEmpty(templateMap)) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Empty content").build();
        }
        REQUIRED_KEYS.forEach(key -> {
            if (!templateMap.containsKey(key)) {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), MessageFormat.format("Key {0} required",  key)).build();
            }
        });
    }

    public static String readYamlFile() {
        try {
            Resource resource = new ClassPathResource("template/error_template.yaml");
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                return FileCopyUtils.copyToString(reader);
            }
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
        }
    }
}
