package com.milesight.beaveriot.dashboard.po;

import com.milesight.beaveriot.dashboard.constants.DashboardDataFieldConstants;
import com.milesight.beaveriot.data.support.MapJsonConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Map;

/**
 * @author loong
 * @date 2024/10/14 15:10
 */
@Data
@Table(name = "t_dashboard_widget")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class DashboardWidgetPO {

    @Id
    private Long id;
    private Long dashboardId;
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
            if (dataStr.length() > DashboardDataFieldConstants.WIDGET_MAX_DATA_SIZE) {
                throw new IllegalArgumentException("Dashboard too large");
            }
        }
    }
}
