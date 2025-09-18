package com.milesight.beaveriot.blueprint.library.service;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.blueprint.library.client.response.ClientResponse;
import com.milesight.beaveriot.blueprint.library.client.utils.OkHttpUtil;
import com.milesight.beaveriot.blueprint.library.config.BlueprintLibraryConfig;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryAddressErrorCode;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryManifest;
import com.milesight.beaveriot.blueprint.library.po.BlueprintLibraryAddressPO;
import com.milesight.beaveriot.blueprint.library.repository.BlueprintLibraryAddressRepository;
import com.milesight.beaveriot.blueprint.library.support.YamlConverter;
import com.milesight.beaveriot.context.model.BlueprintLibraryType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * author: Luxb
 * create: 2025/9/1 10:04
 **/
@Slf4j
@Service
public class BlueprintLibraryAddressService {
    private final BlueprintLibraryConfig blueprintLibraryConfig;
    private final BlueprintLibraryAddressRepository blueprintLibraryAddressRepository;

    public BlueprintLibraryAddressService(BlueprintLibraryConfig blueprintLibraryConfig, BlueprintLibraryAddressRepository blueprintLibraryAddressRepository) {
        this.blueprintLibraryConfig = blueprintLibraryConfig;
        this.blueprintLibraryAddressRepository = blueprintLibraryAddressRepository;
    }

    public List<BlueprintLibraryAddress> getDistinctBlueprintLibraryAddresses() {
        List<BlueprintLibraryAddress> distinctBlueprintLibraryAddresses = new ArrayList<>();

        List<BlueprintLibraryAddress> allTenantsBlueprintLibraryAddresses = findAllIgnoreTenant();
        BlueprintLibraryAddress defaultBlueprintLibraryAddress = blueprintLibraryConfig.getDefaultBlueprintLibraryAddress();
        List<BlueprintLibraryAddress> allBlueprintLibraryAddresses = new ArrayList<>(allTenantsBlueprintLibraryAddresses);
        allBlueprintLibraryAddresses.add(defaultBlueprintLibraryAddress);

        Set<String> keys = new HashSet<>();
        for (BlueprintLibraryAddress blueprintLibraryAddress : allBlueprintLibraryAddresses) {
            if (!defaultBlueprintLibraryAddress.logicEquals(blueprintLibraryAddress) && !blueprintLibraryAddress.getActive()) {
                continue;
            }

            if (!keys.contains(blueprintLibraryAddress.getKey())) {
                keys.add(blueprintLibraryAddress.getKey());
                distinctBlueprintLibraryAddresses.add(blueprintLibraryAddress);
            }
        }
        return distinctBlueprintLibraryAddresses;
    }

    public BlueprintLibraryAddress getDefaultBlueprintLibraryAddress() {
        return blueprintLibraryConfig.getDefaultBlueprintLibraryAddress();
    }

    public BlueprintLibraryAddress getCurrentBlueprintLibraryAddress() {
        BlueprintLibraryAddress activeBlueprintLibraryAddress = findByActiveTrue();
        if (activeBlueprintLibraryAddress == null) {
            activeBlueprintLibraryAddress = blueprintLibraryConfig.getDefaultBlueprintLibraryAddress();
        }
        return activeBlueprintLibraryAddress;
    }

    public BlueprintLibraryAddress findByActiveTrue() {
        return blueprintLibraryAddressRepository.findAllByActiveTrue().stream().map(this::convertPOToModel).findFirst().orElse(null);
    }

    @Transactional
    public void setAllInactive() {
        blueprintLibraryAddressRepository.setAllInactive();
    }

    @Transactional
    public void setActiveOnlyByTypeUrlBranch(String type, String url, String branch) {
        blueprintLibraryAddressRepository.setActiveOnlyByTypeUrlBranch(type, url, branch);
    }

    public List<BlueprintLibraryAddress> findAll() {
        return blueprintLibraryAddressRepository.findAll().stream().map(this::convertPOToModel).toList();
    }

    public List<BlueprintLibraryAddress> findAllIgnoreTenant() {
        return blueprintLibraryAddressRepository.findAllIgnoreTenant().stream().map(this::convertPOToModel).toList();
    }

    public List<BlueprintLibraryAddress> findAllByTypeAndUrlAndBranch(String type, String url, String branch) {
        return blueprintLibraryAddressRepository.findAllByTypeAndUrlAndBranch(type, url, branch).stream().map(this::convertPOToModel).toList();
    }

    public List<BlueprintLibraryAddress> findAllByTypeAndUrlAndBranchIgnoreTenant(String type, String url, String branch) {
        return blueprintLibraryAddressRepository.findAllByTypeAndUrlAndBranchIgnoreTenant(type, url, branch).stream().map(this::convertPOToModel).toList();
    }

    public BlueprintLibraryAddress findByTypeAndUrlAndBranch(String type, String url, String branch) {
        List<BlueprintLibraryAddress> blueprintLibraryAddresses = findAllByTypeAndUrlAndBranch(type, url, branch);
        if (CollectionUtils.isEmpty(blueprintLibraryAddresses)) {
            return null;
        }

        return blueprintLibraryAddresses.get(0);
    }

    public void save(BlueprintLibraryAddress blueprintLibraryAddress) {
        blueprintLibraryAddressRepository.save(convertModelToPO(blueprintLibraryAddress));
    }

    public BlueprintLibraryManifest validateAndGetManifest(BlueprintLibraryAddress blueprintLibraryAddress) {
        if (blueprintLibraryAddress == null) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_NULL).build();
        }

        blueprintLibraryAddress.validate();

        String manifestContent;
        if (BlueprintLibraryType.Zip == blueprintLibraryAddress.getType()) {
            manifestContent = getManifestContentFromZip(blueprintLibraryAddress.getCodeZipUrl(), blueprintLibraryAddress.getManifestFilePath());
        } else {
            String manifestUrl = blueprintLibraryAddress.getRawManifestUrl();
            manifestContent = getManifestContentFromUrl(manifestUrl);
        }

        if (manifestContent == null) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_MANIFEST_NOT_REACHABLE).build();
        }

        BlueprintLibraryManifest manifest = YamlConverter.from(manifestContent, BlueprintLibraryManifest.class);
        if (manifest == null || !manifest.validate()) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_MANIFEST_INVALID).build();
        }

        return manifest;
    }

    private String getManifestContentFromUrl(String manifestUrl) {
        ClientResponse response = OkHttpUtil.get(manifestUrl);
        if (response == null) {
            return null;
        }

        if (!response.isSuccessful()) {
            return null;
        }

        return response.getData();
    }

    private String getManifestContentFromZip(String zipUrl, String manifestFilePath) {
        Request request = new Request.Builder().url(zipUrl).build();
        try (Response response = OkHttpUtil.getClient().newCall(request).execute();
             InputStream inputStream = response.body() != null ? response.body().byteStream() : null) {
            if (inputStream == null) {
                return null;
            }
            try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
                if (!response.isSuccessful()) {
                    return null;
                }

                ZipEntry rootEntry = zipInputStream.getNextEntry();
                if (rootEntry == null) {
                    return null;
                }

                String rootPrefix = rootEntry.getName();
                if (!rootEntry.isDirectory() || !rootPrefix.endsWith("/")) {
                    return null;
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
                    if (relativePath.equals(manifestFilePath)) {
                        return content;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public BlueprintLibraryAddress convertPOToModel(BlueprintLibraryAddressPO blueprintLibraryAddressPO) {
        BlueprintLibraryAddress address = BlueprintLibraryAddress.of(blueprintLibraryAddressPO.getType(), blueprintLibraryAddressPO.getUrl(), blueprintLibraryAddressPO.getBranch());
        address.setId(blueprintLibraryAddressPO.getId());
        address.setActive(blueprintLibraryAddressPO.getActive());
        address.setCreatedAt(blueprintLibraryAddressPO.getCreatedAt());
        return address;
    }

    public BlueprintLibraryAddressPO convertModelToPO(BlueprintLibraryAddress blueprintLibraryAddress) {
        BlueprintLibraryAddressPO addressPO = new BlueprintLibraryAddressPO();
        addressPO.setId(blueprintLibraryAddress.getId());
        addressPO.setType(blueprintLibraryAddress.getType().name());
        addressPO.setUrl(blueprintLibraryAddress.getUrl());
        addressPO.setBranch(blueprintLibraryAddress.getBranch());
        addressPO.setActive(blueprintLibraryAddress.getActive());
        return addressPO;
    }
}