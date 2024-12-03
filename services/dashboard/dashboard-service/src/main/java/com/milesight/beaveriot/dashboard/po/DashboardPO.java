package com.milesight.beaveriot.dashboard.po;

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
@Table(name = "t_dashboard")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class DashboardPO {

    @Id
    private Long id;
    private String name;
    @CreatedDate
    private Long createdAt;
    @LastModifiedDate
    private Long updatedAt;

}
