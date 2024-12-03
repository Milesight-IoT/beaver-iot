package com.milesight.beaveriot.user.po;

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
    private String email;
    private String emailHash;
    private String nickname;
    private String password;
    private String preference;
    @CreatedDate
    private Long createdAt;
    @LastModifiedDate
    private Long updatedAt;

}
