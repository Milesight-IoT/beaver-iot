package com.milesight.beaveriot.dashboard.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author loong
 * @date 2024/10/14 15:09
 */
@Data
@Table(name = "t_dashboard_home")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class DashboardHomePO {

    @Id
    private Long id;
    @Column(insertable = false, updatable = false)
    private String tenantId;
    private Long userId;
    private Long dashboardId;
    @CreatedDate
    private Long createdAt;

}
