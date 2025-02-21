package com.milesight.beaveriot.user.po;

import com.milesight.beaveriot.user.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author loong
 * @date 2024/10/14 8:42
 */
@Data
@Table(name = "t_user")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class UserPO {

    @Id
    private Long id;
    @Column(insertable = false, updatable = false)
    private String tenantId;
    private String email;
    private String emailHash;
    private String nickname;
    private String password;
    private String preference;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR", length = 32)
    private UserStatus status;
    @CreatedDate
    private Long createdAt;
    @LastModifiedDate
    private Long updatedAt;

}
