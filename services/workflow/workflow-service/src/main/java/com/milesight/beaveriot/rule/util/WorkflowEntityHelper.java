package com.milesight.beaveriot.rule.util;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * EntityHelper class.
 *
 * @author simon
 * @date 2025/5/15
 */
@Component("workflowServiceEntityHelper")
public class WorkflowEntityHelper {
    @Autowired
    EntityServiceProvider entityServiceProvider;

    public void checkEntityExist(Collection<String> keys) {
        Set<String> notExist = new HashSet<>(keys);
        notExist.removeAll(entityServiceProvider.findByKeys(keys).keySet());
        if (!notExist.isEmpty()) {
            throw ServiceException
                    .with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Entity not exists: " + String.join(", ", notExist))
                    .build();
        }
    }
}
