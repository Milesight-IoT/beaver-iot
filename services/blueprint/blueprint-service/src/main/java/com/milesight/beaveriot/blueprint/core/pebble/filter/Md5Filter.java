package com.milesight.beaveriot.blueprint.core.pebble.filter;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.Map;


@Component
public class Md5Filter implements BlueprintPebbleFilter {

    @Override
    public String getFilterName() {
        return "md5";
    }

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self,
                        EvaluationContext context, int lineNumber) throws PebbleException {
        if (input == null) {
            return null;
        }

        if (input instanceof String str) {
            return DigestUtils.md5DigestAsHex(str.getBytes());
        } else {
            throw new PebbleException(null, "Need a string to hash\n", lineNumber, self.getName());
        }
    }

    @Override
    public List<String> getArgumentNames() {
        return null;
    }
}
