package com.milesight.beaveriot.user.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author loong
 * @date 2024/11/19 15:41
 */
@Data
@Table(name = "t_user_role")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class UserRolePO {

    @Id
    private Long id;
    @Column(insertable = false, updatable = false)
    private String tenantId;
    private Long userId;
    private Long roleId;
    @CreatedDate
    private Long createdAt;

}
