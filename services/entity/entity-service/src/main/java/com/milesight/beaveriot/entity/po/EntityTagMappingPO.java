package com.milesight.beaveriot.entity.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.*;
import lombok.extern.slf4j.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Data
@Table(name = "t_entity_tag_mapping")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityTagMappingPO {

    @Id
    private Long id;

    @Column(insertable = false, updatable = false)
    private String tenantId;

    private Long entityId;

    private Long tagId;

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long updatedAt;

}
