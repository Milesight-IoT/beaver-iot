package com.milesight.beaveriot.permission.aspect;

import java.lang.annotation.*;

/**
 * @author luxb
 * @date 2025/12/30 17:34
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
@Inherited
public @interface AdminPermission {
}
