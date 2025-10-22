package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.blueprint.library.component.interfaces.BlueprintLibraryAddressProxy;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.model.support.BlueprintLibraryAddressSupport;
import lombok.Data;

import java.io.*;

/**
 * author: Luxb
 * create: 2025/10/21 10:57
 **/
@Data
public class BlueprintLibraryAddressZipProxy implements BlueprintLibraryAddressProxy {
    private String zipFilePath;

    public static BlueprintLibraryAddressZipProxy of(String zipFilePath) {
        return new BlueprintLibraryAddressZipProxy(zipFilePath);
    }

    public BlueprintLibraryAddressZipProxy(String zipFilePath) {
        this.zipFilePath = zipFilePath;
    }

    @Override
    public String getManifestContent() {
        return BlueprintLibraryAddressSupport.getManifestContentFromZip(zipFilePath, BlueprintLibraryAddress.Constants.PATH_MANIFEST, this::getDataInputStreamByZipFilePath);
    }

    @Override
    public InputStream getDataInputStream() {
        return getDataInputStreamByZipFilePath(this.zipFilePath);
    }

    public InputStream getDataInputStreamByZipFilePath(String zipFilePath) {
        try {
            File file = new File(zipFilePath);
            if (!file.exists()) {
                throw new FileNotFoundException("ZIP file not found: " + zipFilePath);
            }

            if (!file.isFile()) {
                throw new IOException("Path is not a file: " + zipFilePath);
            }

            return new BufferedInputStream(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException("Failed to open input stream for ZIP file: " + zipFilePath, e);
        }
    }
}