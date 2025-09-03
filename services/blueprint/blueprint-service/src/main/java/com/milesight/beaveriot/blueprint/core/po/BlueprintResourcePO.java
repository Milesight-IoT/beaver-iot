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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Table(name = "t_blueprint_resource")
@EntityListeners(AuditingEntityListener.class)
public class BlueprintResourcePO {

    @Id
    private Long id;

    private String resourceType;

    private String resourceId;

    private Long blueprintId;

    private String tenantId;

    private Boolean managed;

    @CreatedDate
    private Long createdAt;

    @CreatedBy
    private String createdBy;

}
