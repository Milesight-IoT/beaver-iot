package com.milesight.beaveriot.context.i18n.message;

import com.milesight.beaveriot.context.i18n.locale.LocaleContext;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * author: Luxb
 * create: 2025/8/7 10:48
 **/
public class MergedResourceBundleMessageSource extends ReloadableResourceBundleMessageSource {
    private static final Field cachedPropertiesField;
    private static final String XML_EXTENSION = ".xml";
    private final List<String> fileExtensions = List.of(".properties", XML_EXTENSION);
    private final PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver;
    private final ConcurrentMap<String, PropertiesHolder> cachedProperties;

    static {
        try {
            cachedPropertiesField = ReloadableResourceBundleMessageSource.class.getDeclaredField("cachedProperties");
            cachedPropertiesField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError("Failed to locate 'cachedProperties' field in ReloadableResourceBundleMessageSource: " + e.getMessage());
        }
    }

    public MergedResourceBundleMessageSource(PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver) {
        this.pathMatchingResourcePatternResolver = pathMatchingResourcePatternResolver;
        this.cachedProperties = getCachedProperties();
    }

    /**
     * Retrieves a localized message using the message code, arguments, and default message,
     * using the current thread's locale.
     *
     * @param code the message code, must not be null
     * @param args an array of arguments for message placeholders, may be null
     * @param defaultMessage the default message to return if no message is found, may be null
     * @return the resolved message string
     * @see #getMessage(String, Object[], String, Locale)
     */
    public String getMessage(String code, Object[] args, String defaultMessage) {
        return getMessage(code, args, defaultMessage, LocaleContext.getLocale());
    }

    /**
     * Retrieves a localized message using the message code and arguments,
     * with no default message and using the current thread's locale.
     *
     * @param code the message code, must not be null
     * @param args an array of arguments for message placeholders, may be null
     * @return the resolved message string (or the code if no message is found)
     * @see #getMessage(String, Object[], String, Locale)
     */
    public String getMessage(String code, Object[] args) {
        return getMessage(code, args, LocaleContext.getLocale());
    }

    /**
     * Retrieves a localized message using only the message code,
     * with no arguments, no default message, and using the current thread's locale.
     *
     * @param code the message code, must not be null
     * @return the resolved message string (or the code if no message is found)
     * @see #getMessage(String, Object[], String, Locale)
     */
    public String getMessage(String code) {
        return getMessage(code, null);
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMap<String, PropertiesHolder> getCachedProperties() {
        try {
            return (ConcurrentMap<String, PropertiesHolder>) cachedPropertiesField.get(this);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access 'cachedProperties' field due to access restriction", e);
        }
    }

    @NonNull
    protected PropertiesHolder refreshProperties(@NonNull String filename, @Nullable PropertiesHolder propHolder) {
        long refreshTimestamp = (getCacheMillis() < 0 ? -1 : System.currentTimeMillis());

        try {
            List<Resource> resourceList = new ArrayList<>();
            for (String fileExtension : this.fileExtensions) {
                Resource[] resources = pathMatchingResourcePatternResolver.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + filename + fileExtension);
                if (resources.length > 0) {
                    resourceList.addAll(Arrays.stream(resources).toList());
                }
            }
            propHolder = refreshPropertiesByResources(resourceList, filename, propHolder, refreshTimestamp);
        } catch (IOException e) {
            propHolder = new PropertiesHolder();
        }

        cachedProperties.put(filename, propHolder);
        return propHolder;
    }

    private PropertiesHolder refreshPropertiesByResources(List<Resource> resourceList, String filename, PropertiesHolder propHolder, long refreshTimestamp) {
        if (resourceList.isEmpty()) {
            // Resource does not exist.
            if (logger.isDebugEnabled()) {
                logger.debug("No properties file found for [" + filename + "]");
            }
            // Empty holder representing "not found".
            return new PropertiesHolder();
        }

        List<Properties> propertiesList = new ArrayList<>();
        long latestFileTimestamp = -1;
        if (getCacheMillis() >= 0) {
            // Last-modified timestamp of file will just be read if caching with timeout.
            for (Resource resource : resourceList) {
                long fileTimestamp = -1;
                try {
                    fileTimestamp = resource.lastModified();
                }
                catch (IOException ex) {
                    // Probably a class path resource: cache it forever.
                    if (logger.isDebugEnabled()) {
                        logger.debug(resource + " could not be resolved in the file system - assuming that it hasn't changed", ex);
                    }
                }
                latestFileTimestamp = Math.max(fileTimestamp, latestFileTimestamp);
            }

            if (propHolder != null && propHolder.getFileTimestamp() == latestFileTimestamp) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Re-caching properties for filename [" + filename + "] - file hasn't been modified");
                }
                propHolder.setRefreshTimestamp(refreshTimestamp);
                return propHolder;
            }
        }

        for (Resource resource : resourceList) {
            try {
                Properties props = loadProperties(resource, filename);
                propertiesList.add(props);
            }
            catch (IOException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Could not parse properties file [" + resource.getFilename() + "]", ex);
                }
            }
        }

        if (!CollectionUtils.isEmpty(propertiesList)) {
            Properties mergedProps = getMergedProperties(propertiesList);
            propHolder = new PropertiesHolder(mergedProps, latestFileTimestamp);
        } else {
            // Empty holder representing "not valid".
            propHolder = new PropertiesHolder();
        }

        propHolder.setRefreshTimestamp(refreshTimestamp);
        return propHolder;
    }

    private Properties getMergedProperties(List<Properties> propertiesList) {
        Properties mergedProps = new Properties();
        for (Properties props : propertiesList) {
            for (String key : props.stringPropertyNames()) {
                if (!mergedProps.containsKey(key)) {
                    mergedProps.setProperty(key, props.getProperty(key));
                }
            }
        }
        return mergedProps;
    }
}