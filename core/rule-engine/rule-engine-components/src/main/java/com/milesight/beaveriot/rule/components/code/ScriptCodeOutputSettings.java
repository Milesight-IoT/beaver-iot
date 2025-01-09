package com.milesight.beaveriot.rule.components.code;

import com.milesight.beaveriot.rule.enums.DataTypeEnums;
import lombok.Data;

/**
 * @author leon
 */
@Data
public class ScriptCodeOutputSettings {

    private DataTypeEnums type;
    private String name;

}
