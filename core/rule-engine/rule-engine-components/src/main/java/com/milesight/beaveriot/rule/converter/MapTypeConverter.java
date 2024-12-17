package com.milesight.beaveriot.rule.converter;

import com.milesight.beaveriot.rule.AutowiredTypeConverter;
import com.milesight.beaveriot.rule.support.JsonHelper;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author leon
 */
@Component
public class MapTypeConverter extends AutowiredTypeConverter<String, Map<String, ?>> {

    @Override
    public Map<String, ?> doConvertTo(Class<Map<String, ?>> type, Exchange exchange, String value) throws TypeConversionException {
        if (ObjectUtils.isEmpty(value)) {
            return Map.of();
        }
        return JsonHelper.fromJSON(value, Map.class);
    }
}
