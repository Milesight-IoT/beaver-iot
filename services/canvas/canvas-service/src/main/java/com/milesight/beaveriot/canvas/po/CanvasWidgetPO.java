package com.milesight.beaveriot.canvas.po;

import com.milesight.beaveriot.canvas.constants.CanvasDataFieldConstants;
import com.milesight.beaveriot.data.support.MapJsonConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Map;

/**
 * @author loong
 * @date 2024/10/14 15:10
 */
@Data
@Table(name = "t_canvas_widget")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class CanvasWidgetPO {

    @Id
    private Long id;
    private Long canvasId;
    @Column(insertable = false, updatable = false)
    private String tenantId;
    private Long userId;
    @Convert(converter = MapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> data;
    @CreatedDate
    private Long createdAt;
    @LastModifiedDate
    private Long updatedAt;

    @PreUpdate
    @PrePersist
    private void validateDataSize() {
        if (getData() != null) {
            String dataStr = new MapJsonConverter().convertToDatabaseColumn(getData());
            if (dataStr.length() > CanvasDataFieldConstants.WIDGET_MAX_DATA_SIZE) {
                throw new IllegalArgumentException("Widget too large");
            }
        }
    }
}
