package com.milesight.beaveriot.dashboard.po;

import com.milesight.beaveriot.data.support.MapJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Map;

/**
 * @author loong
 * @date 2024/10/14 15:11
 */
@Data
@Table(name = "t_dashboard_widget_template")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class DashboardWidgetTemplatePO {

    @Id
    private Long id;
    private String name;
    @Column(insertable = false, updatable = false)
    private String tenantId;
    @Convert(converter = MapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> data;
    @CreatedDate
    private Long createdAt;
    @LastModifiedDate
    private Long updatedAt;

}
