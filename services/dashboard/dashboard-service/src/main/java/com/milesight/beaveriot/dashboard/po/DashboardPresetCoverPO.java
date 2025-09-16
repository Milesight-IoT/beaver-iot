package com.milesight.beaveriot.dashboard.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * DashboardPresetCoverPO class.
 *
 * @author simon
 * @date 2025/9/8
 */
@Data
@FieldNameConstants
@Entity
@Table(name = "t_dashboard_preset_cover")
public class DashboardPresetCoverPO {
    @Id
    private Long id;

    private String name;

    private String type;

    private String data;

    private Integer ordered;
}
