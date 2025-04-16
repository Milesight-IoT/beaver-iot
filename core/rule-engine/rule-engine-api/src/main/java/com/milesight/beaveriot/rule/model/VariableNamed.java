package com.milesight.beaveriot.rule.model;

import org.springframework.util.Assert;

/**
 * @author leon
 */
public interface VariableNamed {

    static VariableNamed of(String string) {
        GenericVariable entityDefinition = new GenericVariable();
        entityDefinition.setName(string);
        return entityDefinition;
    }

    String getName();

    default String getIdentify() {
       return null;
    }

    default boolean match(Object key) {
        Assert.notNull(key,"key must not be null");
        return key.equals(getName()) || key.equals(getIdentify());
    }
}
