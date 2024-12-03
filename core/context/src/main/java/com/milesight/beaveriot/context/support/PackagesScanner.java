package com.milesight.beaveriot.context.support;

import com.milesight.beaveriot.base.exception.ConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @author leon
 */
@Slf4j
public class PackagesScanner {

    private static final String RESOURCE_PATTERN = "/**/*.class";

    private static final String CLASSPATH_URL_PREFIX = "classpath*:";

    /**
     * Excluded package path. If the path contains a package string, it is an excluded package path.
     */
    private String[] excludePackages = new String[]{};

    public void doScan(String packageStr, Consumer<Class<?>> consumer) {
        //Make sure to scan only once
        long currentTimeMillis = System.currentTimeMillis();
        try {
            String pattern = CLASSPATH_URL_PREFIX + ClassUtils.convertClassNameToResourcePath(packageStr) + RESOURCE_PATTERN;
            ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resourcePatternResolver.getResources(pattern);
            MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
            for (Resource resource : resources) {
                if (resource.isReadable() && !isExcludePackages(resource.getURL().toString())) {
                    try {
                        MetadataReader reader = readerFactory.getMetadataReader(resource);
                        String className = reader.getClassMetadata().getClassName();
                        Class<?> clazz = resourcePatternResolver.getClassLoader().loadClass(className);
                        consumer.accept(clazz);
                    } catch (Exception e) {
                        log.error("Failed to scan classpath classes:" + e.getMessage());
                    }
                }
            }
        } catch (IOException ex) {
            throw new ConfigurationException("Failed to scan classpath for unlisted classes:" + ex.getMessage());
        } catch (Throwable e) {
            log.error("Failed to scan classpath for unlisted classes:" + e.getMessage());
        }
        log.trace("Scan package {} has been executed, cost time:{}ms", packageStr, System.currentTimeMillis() - currentTimeMillis);
    }

    private boolean isExcludePackages(String path) {
        if (ObjectUtils.isEmpty(excludePackages)) {
            return false;
        }
        return Arrays.stream(excludePackages).anyMatch(path::contains);
    }
}
