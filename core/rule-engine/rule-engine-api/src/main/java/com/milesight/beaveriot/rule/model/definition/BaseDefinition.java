package com.milesight.beaveriot.rule.model.definition;

import lombok.Data;

/**
 * @author leon
 */
@Data
public class BaseDefinition {

    protected String name;
    protected String title;

    public static BaseDefinition create(String name, String title) {
        BaseDefinition baseDefinition = new BaseDefinition();
        baseDefinition.setName(name);
        baseDefinition.setTitle(title);
        return baseDefinition;
    }

}
