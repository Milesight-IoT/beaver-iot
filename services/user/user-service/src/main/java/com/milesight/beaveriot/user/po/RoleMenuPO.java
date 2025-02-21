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
 * @date 2024/11/21 16:10
 */
@Data
@Table(name = "t_role_menu")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class RoleMenuPO {

    @Id
    private Long id;
    private Long roleId;
    private Long menuId;
    @Column(insertable = false, updatable = false)
    private String tenantId;
    @CreatedDate
    private Long createdAt;

}
