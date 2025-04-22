package com.milesight.beaveriot.scheduler.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;


@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Table(name = "t_schedule_settings")
@EntityListeners(AuditingEntityListener.class)
public class ScheduleSettingsPO {

    @Id
    private Long id;

    @Column(insertable = false, updatable = false)
    private String tenantId;

    @NotEmpty
    private String taskKey;

    @NonNull
    @Enumerated(EnumType.STRING)
    private ScheduleType scheduleType;

    @NotEmpty
    private String scheduleRule;

    @Nullable
    private String payload;

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long updatedAt;

}
