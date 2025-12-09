package com.milesight.beaveriot.canvas.po;

import com.milesight.beaveriot.canvas.enums.CanvasAttachType;
import com.milesight.beaveriot.data.support.MapJsonConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Map;

/**
 * CanvasPO class.
 *
 * @author simon
 * @date 2025/9/8
 */
@Data
@FieldNameConstants
@Entity
@Table(name = "t_canvas")
@EntityListeners(AuditingEntityListener.class)
public class CanvasPO {
    @Id
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private CanvasAttachType attachType;

    private String attachId;

    @Column(insertable = false, updatable = false)
    private String tenantId;

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long updatedAt;

    @Convert(converter = MapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> attributes;
}
