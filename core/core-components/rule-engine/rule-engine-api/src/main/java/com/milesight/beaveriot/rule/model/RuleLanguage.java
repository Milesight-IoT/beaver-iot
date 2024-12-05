package com.milesight.beaveriot.rule.model;

import lombok.Data;

import java.util.List;

/**
 * @author leon
 */
@Data
public class RuleLanguage {

    private List<String> code;

    private List<String> expression;

}
