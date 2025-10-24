package com.milesight.beaveriot.blueprint.core.pebble.function;

import com.google.common.primitives.Longs;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FindEntityFunction implements BlueprintPebbleFunction {

    @Lazy
    @Autowired
    private EntityServiceProvider entityServiceProvider;

    @Override
    public String getFunctionName() {
        return "find_entity";
    }

    @Override
    public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
        var arg = args.get("0");
        if (arg instanceof Number id) {
            return entityServiceProvider.findById(id.longValue());
        } else if (arg instanceof String key) {
            var id = Longs.tryParse(key);
            if (id != null) {
                return entityServiceProvider.findById(id);
            } else {
                return entityServiceProvider.findByKey(key);
            }
        } else {
            return null;
        }
    }

    @Override
    public List<String> getArgumentNames() {
        return null;
    }
}
