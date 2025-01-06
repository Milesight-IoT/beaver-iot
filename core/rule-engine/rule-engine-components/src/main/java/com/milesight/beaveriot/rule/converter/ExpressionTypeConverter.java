package com.milesight.beaveriot.rule.converter;

import com.milesight.beaveriot.rule.AutowiredTypeConverter;
import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import com.milesight.beaveriot.rule.support.JsonHelper;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

/**
 * @author leon
 */
@Component
public class ExpressionTypeConverter extends AutowiredTypeConverter<String, ExpressionNode> {

    @Override
    public ExpressionNode doConvertTo(Class<ExpressionNode> type, Exchange exchange, String value) throws TypeConversionException {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        return JsonHelper.fromJSON(value, ExpressionNode.class);
    }

}
