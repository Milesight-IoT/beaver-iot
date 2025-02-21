package com.milesight.beaveriot.entity.po;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
    @Column(insertable = false, updatable = false)
    private String tenantId;
    private Long entityId;
    private Long valueLong;
    private Double valueDouble;
    private Boolean valueBoolean;
    @Column(length = 10485760) // 10 MB
    private String valueString;
    private byte[] valueBinary;
    private Long timestamp;
    private Long updatedAt;

    public void setValue(EntityValueType valueType, Object value) {
        if (value == null) {
            return;
        }
        switch (valueType) {
            case OBJECT:
                // do nothing
                break;
            case BOOLEAN:
                this.valueBoolean = (Boolean) valueType.convertValue(value);
                break;
            case LONG:
                this.valueLong = (Long) valueType.convertValue(value);
                break;
            case STRING:
                this.valueString = (String) valueType.convertValue(value);
                break;
            case DOUBLE:
                this.valueDouble = (Double) valueType.convertValue(value);
                break;
            case BINARY:
                this.valueBinary = (byte[]) valueType.convertValue(value);
                break;
            default:
                throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
    }

}
