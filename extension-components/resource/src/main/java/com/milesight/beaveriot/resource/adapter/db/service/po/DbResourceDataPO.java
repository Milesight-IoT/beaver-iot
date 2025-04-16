package com.milesight.beaveriot.resource.adapter.db.service.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * DBResourceObject class.
 *
 * @author simon
 * @date 2025/4/7
 */
@Data
@Entity
@FieldNameConstants
@Table(name = "t_resource_data")
@EntityListeners(AuditingEntityListener.class)
public class DbResourceDataPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "obj_key", length = 512)
    private String objKey;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "content_length")
    private Long contentLength;

    @Column(name = "data")
    private byte[] data;

    @Column(name = "created_at")
    @CreatedDate
    private Long createdAt;
}
