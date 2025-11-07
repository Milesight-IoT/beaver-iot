package com.milesight.beaveriot.context.integration.support;

import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.ValueStoreMod;

/**
 * author: Luxb
 * create: 2025/11/7 15:07
 **/
public class ValueStoreModSupport {
    public static ValueStoreMod format(EntityType type, ValueStoreMod valueStoreMod) {
        if (valueStoreMod == null) {
            valueStoreMod = ValueStoreMod.ALL;
        }

        switch (type) {
            case SERVICE, EVENT: {
                if (valueStoreMod == ValueStoreMod.ALL) {
                    valueStoreMod = ValueStoreMod.HISTORY;
                } else if (valueStoreMod == ValueStoreMod.LATEST) {
                    valueStoreMod = ValueStoreMod.NONE;
                }
            }
        }
        return valueStoreMod;
    }
}
