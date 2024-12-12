package com.milesight.beaveriot.rule.model;

import lombok.Data;

import java.util.List;

/**
 * @author leon
 */
@Data
public class RuleLanguage {

    public static final String LANGUAGE_SPEL = "spel";

    public static final String LANGUAGE_CONDITION = "condition";

    private List<String> code;

    private List<String> expression;

}
