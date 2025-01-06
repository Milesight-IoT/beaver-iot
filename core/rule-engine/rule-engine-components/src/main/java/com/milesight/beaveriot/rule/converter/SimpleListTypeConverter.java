package com.milesight.beaveriot.rule.converter;

import com.milesight.beaveriot.rule.AutowiredTypeConverter;
import com.milesight.beaveriot.rule.support.JsonHelper;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * @author leon
 */
@Component
public class SimpleListTypeConverter extends AutowiredTypeConverter<String, List<?>> {

    private ConversionService conversionService;

    public SimpleListTypeConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public List<?> doConvertTo(Class<List<?>> type, Exchange exchange, String value) throws TypeConversionException {
        if (ObjectUtils.isEmpty(value)) {
            return List.of();
        }
        String trimValue = value.trim();
        if(trimValue.startsWith("[") && trimValue.endsWith("]")){
            return JsonHelper.fromJSON(value, List.class);
        } else {
            return conversionService.convert(value, type);
        }

    }
}
