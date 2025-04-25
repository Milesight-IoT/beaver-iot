package com.milesight.beaveriot.scheduler.core.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.*;
import org.checkerframework.checker.index.qual.NonNegative;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.NonNull;


@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Table(name = "t_scheduled_task")
@EntityListeners(AuditingEntityListener.class)
public class ScheduledTaskPO {

    @Id
    private Long id;

    @NonNull
    private Long executionEpochSecond;

    @NotEmpty
    private String taskKey;

    @NonNull
    @NonNegative
    private Integer attempts;

    @NonNull
    @NonNegative
    private Integer iteration;

    @NonNull
    @NonNegative
    private Long triggeredAt;

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long updatedAt;

}
