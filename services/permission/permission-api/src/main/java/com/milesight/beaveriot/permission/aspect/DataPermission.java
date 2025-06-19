package com.milesight.beaveriot.permission.aspect;

import com.milesight.beaveriot.permission.enums.ColumnDataType;
import com.milesight.beaveriot.permission.enums.DataPermissionType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author loong
 * @date 2024/12/5 11:57
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
@Inherited
public @interface DataPermission {

    DataPermissionType type();
    ColumnDataType dataType() default ColumnDataType.NUMBER;
    String column();
}
