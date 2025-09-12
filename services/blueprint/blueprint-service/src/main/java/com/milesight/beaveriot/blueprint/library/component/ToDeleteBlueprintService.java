package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.blueprint.facade.IBlueprintFacade;
import com.milesight.beaveriot.blueprint.support.TemplateLoader;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/9 16:18
 **/
@Service
public class ToDeleteBlueprintService implements IBlueprintFacade {
    @Override
    public Long deployBlueprint(TemplateLoader templateLoader, Map<String, Object> variables) {
        return 9999999999L;
    }
}
