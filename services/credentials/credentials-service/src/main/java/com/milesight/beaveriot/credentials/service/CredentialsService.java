package com.milesight.beaveriot.credentials.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.CredentialsServiceProvider;
import com.milesight.beaveriot.context.integration.model.Credentials;
import com.milesight.beaveriot.context.security.SecurityUserContext;
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
        var credentialsPO = credentialsRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.DATA_NO_FOUND));
        val operatorId = SecurityUserContext.getUserId() == null ? null : SecurityUserContext.getUserId().toString();
        credentialsPO.setDescription(request.getDescription());
        credentialsPO.setAccessKey(request.getAccessKey());
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

    private static String getCredentialsDefaultAccessKeyByType(String credentialsType) {
        return credentialsType.toLowerCase();
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

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Credentials getOrCreateDefaultCredentials(String credentialsType) {
        return getOrCreateCredentials(credentialsType, getCredentialsDefaultAccessKeyByType(credentialsType));
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Credentials getOrCreateCredentials(String credentialsType, String username) {
        Assert.notNull(credentialsType, "credentialsType cannot be null");
        Assert.notNull(username, "username cannot be null");

        var credentials = getCredentials(credentialsType, username).orElse(null);
        if (credentials == null) {
            synchronized (this) {
                credentials = getCredentials(credentialsType, username).orElse(null);
                if (credentials == null) {
                    addCredentials(Credentials.builder()
                            .credentialsType(credentialsType)
                            .accessKey(username)
                            .accessSecret(SecretUtils.randomSecret(32))
                            .build());
                    credentials = getCredentials(credentialsType, username)
                            .orElseThrow(() -> new ServiceException(ErrorCode.DATA_NO_FOUND, "credentials not found"));
                }
            }
        }
        return credentials;
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

}
