package com.milesight.beaveriot.credentials.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.CredentialsServiceProvider;
import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.context.integration.model.Credentials;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.context.util.SecretUtils;
import com.milesight.beaveriot.credentials.api.model.CredentialsCacheInvalidationEvent;
import com.milesight.beaveriot.credentials.api.model.CredentialsChangeEvent;
import com.milesight.beaveriot.credentials.model.request.AddCredentialsRequest;
import com.milesight.beaveriot.credentials.model.request.BatchDeleteCredentialsRequest;
import com.milesight.beaveriot.credentials.model.request.SearchCredentialsRequest;
import com.milesight.beaveriot.credentials.model.request.UpdateCredentialsRequest;
import com.milesight.beaveriot.credentials.model.response.CredentialsResponse;
import com.milesight.beaveriot.credentials.po.CredentialsPO;
import com.milesight.beaveriot.credentials.repository.CredentialsRepository;
import com.milesight.beaveriot.pubsub.MessagePubSub;
import lombok.extern.slf4j.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;


@Slf4j
@Service
public class CredentialsService implements CredentialsServiceProvider {

    @Autowired
    private MessagePubSub messagePubSub;

    @Autowired
    private CredentialsRepository credentialsRepository;

    private static String getCredentialsDefaultAccessKeyByType(String credentialsType) {
        return "%s@%s".formatted(credentialsType.toLowerCase(), TenantContext.getTenantId());
    }

    public Page<CredentialsResponse> searchCredentials(SearchCredentialsRequest request) {
        if (request.getSort().getOrders().isEmpty()) {
            request.sort(new Sorts().desc(CredentialsPO.Fields.updatedAt));
        }
        return credentialsRepository.findAll(f -> f.eq(CredentialsPO.Fields.credentialsType, request.getCredentialsType())
                                .eq(CredentialsPO.Fields.visible, true),
                        request.toPageable())
                .map(this::convertPOToResponse);
    }

    private CredentialsResponse convertPOToResponse(CredentialsPO po) {
        return CredentialsResponse.builder()
                .id(String.valueOf(po.getId()))
                .tenantId(po.getTenantId())
                .credentialsType(po.getCredentialsType())
                .description(po.getDescription())
                .accessKey(po.getAccessKey())
                .accessSecret(po.getAccessSecret())
                .additionalData(po.getAdditionalData())
                .editable(po.getEditable())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private Credentials convertPOToDTO(CredentialsPO po) {
        return Credentials.builder()
                .id(po.getId())
                .credentialsType(po.getCredentialsType())
                .accessKey(po.getAccessKey())
                .accessSecret(po.getAccessSecret())
                .additionalData(po.getAdditionalData())
                .build();
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void addCredentials(Credentials credentials) {
        addCredentials(AddCredentialsRequest.builder()
                .credentialsType(credentials.getCredentialsType())
                .accessKey(credentials.getAccessKey())
                .accessSecret(credentials.getAccessSecret())
                .additionalData(JsonUtils.fromJSON(credentials.getAdditionalData()))
                .build());
    }

    @Transactional(rollbackFor = Throwable.class)
    public CredentialsResponse addCredentials(AddCredentialsRequest request) {
        log.info("add credentials, accessKey: '{}', tenantId: '{}'", request.getAccessKey(), TenantContext.getTenantId());

        val operatorId = SecurityUserContext.getUserId() == null ? null : SecurityUserContext.getUserId().toString();
        val credentialsPO = credentialsRepository.save(CredentialsPO.builder()
                .id(SnowflakeUtil.nextId())
                .credentialsType(request.getCredentialsType())
                .description(request.getDescription())
                .accessKey(request.getAccessKey())
                .accessSecret(request.getAccessSecret())
                .additionalData(JsonUtils.toJSON(request.getAdditionalData()))
                .editable(true)
                .visible(true)
                .createdBy(operatorId)
                .updatedBy(operatorId)
                .build());
        publishCredentialsChangeEvent(CredentialsChangeEvent.Operation.ADD, convertPOToDTO(credentialsPO), System.currentTimeMillis());
        return convertPOToResponse(credentialsPO);
    }

    @Transactional(rollbackFor = Throwable.class)
    public CredentialsResponse updateCredentials(Long id, UpdateCredentialsRequest request) {
        log.info("update credentials, id: '{}', tenantId: '{}'", id, TenantContext.getTenantId());

        var credentialsPO = credentialsRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.DATA_NO_FOUND));
        val operatorId = SecurityUserContext.getUserId() == null ? null : SecurityUserContext.getUserId().toString();
        credentialsPO.setDescription(request.getDescription());
        credentialsPO.setAccessSecret(request.getAccessSecret());
        credentialsPO.setAdditionalData(JsonUtils.toJSON(request.getAdditionalData()));
        credentialsPO.setUpdatedBy(operatorId);
        credentialsPO = credentialsRepository.save(credentialsPO);

        val currentMillis = System.currentTimeMillis();
        val credentials = convertPOToDTO(credentialsPO);
        publishCredentialsChangeEvent(CredentialsChangeEvent.Operation.DELETE, credentials, currentMillis);
        publishCredentialsChangeEvent(CredentialsChangeEvent.Operation.ADD, credentials, currentMillis);
        publishCredentialsCacheInvalidationEvent(credentials, currentMillis);
        return convertPOToResponse(credentialsPO);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void batchDeleteCredentials(BatchDeleteCredentialsRequest request) {
        batchDeleteCredentials(request.getIds());
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void batchDeleteCredentials(List<Long> ids) {
        log.info("batch delete credentials, ids: '{}', tenantId: '{}'", ids, TenantContext.getTenantId());

        if (ids == null || ids.isEmpty()) {
            return;
        }

        val currentMillis = System.currentTimeMillis();
        val credentialsToBeDeleted = credentialsRepository.findAllById(ids);
        val idsToBeDeleted = credentialsToBeDeleted.stream().map(CredentialsPO::getId).toList();
        credentialsRepository.deleteAllById(idsToBeDeleted);

        val operation = CredentialsChangeEvent.Operation.DELETE;
        credentialsToBeDeleted.forEach(po -> {
            val credentials = convertPOToDTO(po);
            publishCredentialsChangeEvent(operation, credentials, currentMillis);
            publishCredentialsCacheInvalidationEvent(credentials, currentMillis);
        });
    }

    private void publishCredentialsChangeEvent(CredentialsChangeEvent.Operation operation, Credentials po, long currentMillis) {
        messagePubSub.publish(new CredentialsChangeEvent(operation, po, currentMillis));
    }

    private void publishCredentialsCacheInvalidationEvent(Credentials po, long currentMillis) {
        messagePubSub.publishAfterCommit(new CredentialsCacheInvalidationEvent(po, currentMillis));
    }

    public CredentialsResponse getCredentialsResponse(String credentialsType) {
        return credentialsRepository.findFirstByCredentialsTypeAndAccessKey(credentialsType, getCredentialsDefaultAccessKeyByType(credentialsType))
                .map(this::convertPOToResponse)
                .orElseThrow(() -> new ServiceException(ErrorCode.DATA_NO_FOUND));
    }

    @Transactional(rollbackFor = Throwable.class)
    public CredentialsResponse getOrCreateCredentialsResponse(String credentialsType, Boolean autoGeneratePassword) {
        val password = Boolean.TRUE.equals(autoGeneratePassword) ? SecretUtils.randomSecret(32) : "";
        val credentials = getOrCreateCredentials(credentialsType, password);
        return getCredentialsResponse(credentials.getId());
    }

    public CredentialsResponse getCredentialsResponse(Long id) {
        return credentialsRepository.findById(id)
                .map(this::convertPOToResponse)
                .orElseThrow(() -> new ServiceException(ErrorCode.DATA_NO_FOUND));
    }

    public Optional<Credentials> getCredentials(String credentialsType) {
        return credentialsRepository.findFirstByCredentialsTypeAndAccessKey(credentialsType, getCredentialsDefaultAccessKeyByType(credentialsType))
                .map(this::convertPOToDTO);
    }

    @Override
    public Optional<Credentials> getCredentials(CredentialsType credentialType) {
        return getCredentials(credentialType.name());
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Credentials getOrCreateCredentials(CredentialsType credentialType, String password) {
        return getOrCreateCredentials(credentialType.name(), password);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Credentials getOrCreateCredentials(String credentialType) {
        return getOrCreateCredentials(credentialType, SecretUtils.randomSecret(32));
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Credentials getOrCreateCredentials(String credentialsType, String password) {
        return getOrCreateCredentials(credentialsType, getCredentialsDefaultAccessKeyByType(credentialsType), password);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Credentials getOrCreateCredentials(CredentialsType credentialType) {
        return getOrCreateCredentials(credentialType, SecretUtils.randomSecret(32));
    }

    @Transactional(rollbackFor = Throwable.class)
    public Credentials getOrCreateCredentials(String credentialsType, String username, String password) {
        Assert.notNull(credentialsType, "credentialsType cannot be null");
        Assert.notNull(username, "username cannot be null");

        if (password == null) {
            password = "";
        }

        var credentials = getCredentials(credentialsType, username).orElse(null);
        if (credentials == null) {
            synchronized (this) {
                credentials = getCredentials(credentialsType, username).orElse(null);
                if (credentials == null) {
                    addCredentials(Credentials.builder()
                            .credentialsType(credentialsType)
                            .accessKey(username)
                            .accessSecret(password)
                            .build());
                    credentials = getCredentials(credentialsType, username)
                            .orElseThrow(() -> new ServiceException(ErrorCode.DATA_NO_FOUND, "credentials not found"));
                }
            }
        }
        return credentials;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Credentials getOrCreateCredentials(CredentialsType credentialType, String username, String password) {
        return getOrCreateCredentials(credentialType.name(), username, password);
    }

    public Optional<Credentials> getCredentials(Long id) {
        return credentialsRepository.findById(id)
                .map(this::convertPOToDTO);
    }

    @Override
    public Optional<Credentials> getCredentials(String credentialsType, String accessKey) {
        return credentialsRepository.findFirstByCredentialsTypeAndAccessKey(credentialsType, accessKey)
                .map(this::convertPOToDTO);
    }

    @Override
    public Optional<Credentials> getCredentials(CredentialsType credentialType, String accessKey) {
        return getCredentials(credentialType.name(), accessKey);
    }

}
