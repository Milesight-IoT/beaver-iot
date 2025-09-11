package com.milesight.beaveriot.canvas.po;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Entity
@Table(name = "t_canvas_entity")
@EntityListeners(AuditingEntityListener.class)
public class CanvasEntityPO {

    @Id
    private Long id;

    private Long canvasId;

    private Long entityId;

    private String entityKey;

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long updatedAt;

}
