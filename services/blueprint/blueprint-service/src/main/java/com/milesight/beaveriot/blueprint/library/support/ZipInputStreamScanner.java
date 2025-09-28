package com.milesight.beaveriot.blueprint.library.support;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * author: Luxb
 * create: 2025/9/28 16:34
 **/
@Slf4j
public class ZipInputStreamScanner {
    /**
     * Scans a ZIP input stream and processes each entry.
     *
     * @param inputStream the input stream containing a ZIP file
     * @param contentHandler a function that accepts the relative path and content of each entry;
     *                       return {@code true} to continue scanning, or {@code false} to stop
     * @return {@code true} if scanning completes successfully, {@code false} if an error occurs
     */
    public static boolean scan(InputStream inputStream, BiFunction<String, String, Boolean> contentHandler) {
        if (inputStream == null) {
            return false;
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry rootEntry = zipInputStream.getNextEntry();
            if (rootEntry == null) {
                return false;
            }

            String rootPrefix = rootEntry.getName();
            if (!rootEntry.isDirectory() || !rootPrefix.endsWith("/")) {
                return false;
            }
            zipInputStream.closeEntry();

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zipInputStream.closeEntry();
                    continue;
                }

                String entryName = entry.getName();
                if (!entryName.startsWith(rootPrefix)) {
                    zipInputStream.closeEntry();
                    continue;
                }

                String relativePath = entryName.substring(rootPrefix.length());
                if (relativePath.isEmpty()) {
                    zipInputStream.closeEntry();
                    continue;
                }

                byte[] bytes = zipInputStream.readAllBytes();
                String content = new String(bytes, StandardCharsets.UTF_8);
                zipInputStream.closeEntry();

                if (!contentHandler.apply(relativePath, content)) {
                    return true;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
