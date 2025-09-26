package com.milesight.beaveriot.blueprint.core.po;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Table(name = "t_blueprint")
@EntityListeners(AuditingEntityListener.class)
public class BlueprintPO {

    @Id
    private Long id;

    private String tenantId;

    private String description;

    private String chart;

    @CreatedDate
    private Long createdAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedDate
    private Long updatedAt;

    @LastModifiedBy
    private String updatedBy;

}
