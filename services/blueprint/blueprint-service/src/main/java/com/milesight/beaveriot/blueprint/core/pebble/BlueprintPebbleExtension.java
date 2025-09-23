package com.milesight.beaveriot.blueprint.core.pebble;

import com.milesight.beaveriot.blueprint.core.pebble.function.BlueprintPebbleFunction;
import io.pebbletemplates.pebble.extension.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BlueprintPebbleExtension extends AbstractBlueprintPebbleExtension {

    @Autowired
    private List<BlueprintPebbleFunction> functions;

    @Override
    public Map<String, Function> getFunctions() {
        return functions.stream()
                .collect(Collectors.toMap(BlueprintPebbleFunction::getFunctionName, v -> v));
    }
}
