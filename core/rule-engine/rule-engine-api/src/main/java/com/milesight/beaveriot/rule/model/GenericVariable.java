package com.milesight.beaveriot.rule.model;

import lombok.Data;

/**
 * @author leon
 */
@Data
public class GenericVariable implements VariableNamed {

    private String name;
    private String identify;
    private String type;

}
