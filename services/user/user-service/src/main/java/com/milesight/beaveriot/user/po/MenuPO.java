package com.milesight.beaveriot.user.po;

import com.milesight.beaveriot.user.enums.MenuType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author loong
 * @date 2024/11/21 16:08
 */
@Data
@Table(name = "t_menu")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class MenuPO {

    @Id
    private Long id;
    @Column(insertable = false, updatable = false)
    private String tenantId;
    private Long parentId;
    private String name;
    private String code;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar", length = 32)
    private MenuType type;
    @CreatedDate
    private Long createdAt;
    @LastModifiedDate
    private Long updatedAt;

}
