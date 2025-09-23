package com.milesight.beaveriot.blueprint.core.pebble.function;

import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RandomStringFunction implements BlueprintPebbleFunction {
    @Override
    public String getFunctionName() {
        return "random_string";
    }

    @Override
    public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
        var arg1 = args.get("0");
        if (!(arg1 instanceof Number count)) {
            return null;
        }

        var arg2 = args.get("1");
        if (!(arg2 instanceof String chars)) {
            return RandomStringUtils.random(count.intValue());
        } else {
            return RandomStringUtils.random(count.intValue(), chars);
        }
    }

    @Override
    public List<String> getArgumentNames() {
        return null;
    }
}
