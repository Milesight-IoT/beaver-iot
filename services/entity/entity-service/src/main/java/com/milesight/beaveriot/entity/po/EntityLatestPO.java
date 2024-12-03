package com.milesight.beaveriot.entity.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

/**
 * @author loong
 * @date 2024/10/16 14:28
 */
@Data
@Table(name = "t_entity_latest")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class EntityLatestPO {

    @Id
    private Long id;
    private Long entityId;
    private Long valueLong;
    private BigDecimal valueDouble;
    private Boolean valueBoolean;
    @Column(length = 1024)
    private String valueString;
    private byte[] valueBinary;
    private Long timestamp;
    private Long updatedAt;

}
