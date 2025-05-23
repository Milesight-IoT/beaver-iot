package com.milesight.beaveriot.credentials.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Table(name = "t_credentials")
@EntityListeners(AuditingEntityListener.class)
public class CredentialsPO {

    @Id
    private Long id;

    @Column(insertable = false, updatable = false)
    private String tenantId;

    private String credentialsType;

    private String description;

    private String accessKey;

    private String accessSecret;

    @Size(max = 1024)
    private String additionalData;

    private String cryptographicAlgorithm;

    private Boolean editable;

    private Boolean visible;

    @CreatedDate
    private Long createdAt;

    private String createdBy;

    @LastModifiedDate
    private Long updatedAt;

    private String updatedBy;
}
