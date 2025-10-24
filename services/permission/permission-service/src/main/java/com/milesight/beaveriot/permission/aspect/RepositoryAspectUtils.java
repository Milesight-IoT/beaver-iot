package com.milesight.beaveriot.permission.aspect;

import com.milesight.beaveriot.base.utils.TypeUtil;
import jakarta.persistence.Table;
import lombok.experimental.UtilityClass;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.Repository;

@UtilityClass
public class RepositoryAspectUtils {

    public static String getTableName(Class<?> repositoryInterface) {
        if(Repository.class.isAssignableFrom(repositoryInterface)){
            Class<?> repositoryClassType = (Class<?>) TypeUtil.getTypeArgument(repositoryInterface, 0);
            if (repositoryClassType != null) {
                Table annotation = AnnotationUtils.getAnnotation(repositoryClassType, Table.class);
                if (annotation != null) {
                    return annotation.name();
                }
            }
        }
        return null;
    }

}
