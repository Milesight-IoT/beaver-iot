package com.milesight.beaveriot.context.support;

import com.milesight.beaveriot.base.constants.StringConstant;
import com.milesight.beaveriot.base.exception.BootstrapException;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.apache.camel.model.rest.RestParamType.path;

/**
 * @author leon
 */
@Slf4j
public class YamlPropertySourceFactory extends DefaultPropertySourceFactory {

    private static final String PATH_BOOT_INF = "BOOT-INF";

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        if (resource == null) {
            throw new BootstrapException("Yaml PropertySource not found:" + name);
        }
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(name, resource.getResource());
        return sources.get(0);
    }

    public PropertySource<?> createJarPropertySource(String name, String codeSourceLocationPath) throws IOException {

        log.debug("ready load integration.yaml, integration location: {}", codeSourceLocationPath);

        if (codeSourceLocationPath.contains(".jar")) {
            return createNestedJarPropertySource(name, codeSourceLocationPath);
        } else {
            FileUrlResource fileUrlResource = new FileUrlResource(getFullLocationPath(codeSourceLocationPath, IntegrationConstants.INTEGRATION_YAML));
            if (!fileUrlResource.exists()) {
                fileUrlResource = new FileUrlResource(getFullLocationPath(codeSourceLocationPath, IntegrationConstants.INTEGRATION_YML));
                if (!fileUrlResource.exists()) {
                    log.error("Integration yaml not found, please check integration.yaml ：" + getFullLocationPath(codeSourceLocationPath, IntegrationConstants.INTEGRATION_YAML));
                    throw new BootstrapException("Integration yaml not found, please check integration.yaml");
                }
            }
            return createPropertySource(name, new EncodedResource(fileUrlResource, StandardCharsets.UTF_8));
        }
    }

    private String getFullLocationPath(String codeSourceLocationPath, String integrationFileName) {
        return codeSourceLocationPath.endsWith(StringConstant.SLASH) ? codeSourceLocationPath + integrationFileName : codeSourceLocationPath + StringConstant.SLASH + integrationFileName;
    }

    private PropertySource<?> createNestedJarPropertySource(String name, String codeSourceLocationPath) throws IOException {
        codeSourceLocationPath = codeSourceLocationPath.replace("!", "");
        codeSourceLocationPath = replaceJarSchema(codeSourceLocationPath);
        String[] nestedJarPaths = codeSourceLocationPath.split(PATH_BOOT_INF);
        try (FileSystem rootJarFileSystem = FileSystems.newFileSystem(URI.create(nestedJarPaths[0]), Collections.emptyMap())) {
            FileSystem integrationJarFileSystem = null;
            InputStream inputStream = null;

            if (nestedJarPaths.length == 2) {
                if (nestedJarPaths[1].contains(".jar")) {
                    String realJarPath = PATH_BOOT_INF + nestedJarPaths[1];
                    Path jarFileSystemPath = rootJarFileSystem.getPath(realJarPath);
                    integrationJarFileSystem = FileSystems.newFileSystem(jarFileSystemPath, Collections.emptyMap());
                    inputStream = openInputStreamCanTry(integrationJarFileSystem, IntegrationConstants.INTEGRATION_YAML, IntegrationConstants.INTEGRATION_YML);
                } else {
                    String integrationPathPrefix = PATH_BOOT_INF + nestedJarPaths[1];
                    inputStream = openInputStreamCanTry(rootJarFileSystem, integrationPathPrefix + IntegrationConstants.INTEGRATION_YAML, integrationPathPrefix + IntegrationConstants.INTEGRATION_YML);
                }
            } else {
                inputStream = openInputStreamCanTry(rootJarFileSystem, IntegrationConstants.INTEGRATION_YAML, IntegrationConstants.INTEGRATION_YML);
            }

            PropertySource<?> propertySource = createPropertySource(name, new EncodedResource(new InputStreamResource(inputStream), StandardCharsets.UTF_8));
            propertySource.getSource();

            closeFileSystem(integrationJarFileSystem);
            return propertySource;
        }
    }

    private String replaceJarSchema(String codeSourceLocationPath) {
        return codeSourceLocationPath.startsWith("nested:") ?
                codeSourceLocationPath.replace("nested:", "jar:file:") :
                "jar:file:" + codeSourceLocationPath;
    }

    private void closeFileSystem(FileSystem fileSystem) throws IOException {
        if (fileSystem != null && fileSystem.isOpen()) {
            fileSystem.close();
        }
    }

    private InputStream openInputStreamCanTry(FileSystem jarFileSystem, String integrationYaml, String integrationYml) throws IOException {
        InputStream inputStream;
        try {
            Path integrationPath = jarFileSystem.getPath(integrationYaml);
            inputStream = Files.newInputStream(integrationPath);
        } catch (Exception ex) {
            try {
                Path integrationPath = jarFileSystem.getPath(integrationYml);
                inputStream = Files.newInputStream(integrationPath);
            } catch (Exception e) {
                log.error("Integration yaml not found, please check integration.yaml ：" + path + IntegrationConstants.INTEGRATION_YAML);
                throw new BootstrapException("Integration yaml not found, please check integration.yaml");
            }
        }
        return inputStream;
    }

}
