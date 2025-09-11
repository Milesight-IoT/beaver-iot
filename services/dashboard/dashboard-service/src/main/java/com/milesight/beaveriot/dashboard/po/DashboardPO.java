package com.milesight.beaveriot.dashboard.po;

import com.milesight.beaveriot.dashboard.enums.DashboardCoverType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author loong
 * @date 2024/10/14 15:09
 */
@Data
@Table(name = "t_dashboard")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class DashboardPO {

    @Id
    private Long id;
    @Column(insertable = false, updatable = false)
    private String tenantId;
    private Long userId;
    private String name;
    private Long mainCanvasId;
    private String description;
    @Enumerated(EnumType.STRING)
    private DashboardCoverType coverType;
    private String coverData;
    @CreatedDate
    private Long createdAt;
    @LastModifiedDate
    private Long updatedAt;

}
