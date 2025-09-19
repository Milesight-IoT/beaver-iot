package com.milesight.beaveriot.canvas.po;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * CanvasDevicePO class.
 *
 * @author simon
 * @date 2025/9/18
 */
@Data
@FieldNameConstants
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_canvas_device")
@EntityListeners(AuditingEntityListener.class)
public class CanvasDevicePO {
    @Id
    private Long id;

    private Long canvasId;

    private Long deviceId;

    @CreatedDate
    private Long createdAt;
}
