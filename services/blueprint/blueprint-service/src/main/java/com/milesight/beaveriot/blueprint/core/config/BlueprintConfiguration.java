package com.milesight.beaveriot.blueprint.core.config;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.Extension;
import io.pebbletemplates.pebble.loader.StringLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.unit.DataSize;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.List;


@Configuration
public class BlueprintConfiguration {

    @Bean
    @Primary
    public PebbleEngine blueprintPebbleEngine(List<BlueprintPebbleExtension> blueprintPebbleExtensions) {
        return new PebbleEngine.Builder()
                .loader(new StringLoader())
                .extension(blueprintPebbleExtensions.toArray(new Extension[0]))
                .strictVariables(false)
                .newLineTrimming(false)
                .maxRenderedSize((int) DataSize.ofMegabytes(10).toBytes())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public BlueprintIntrinsicsYamlConstructor blueprintIntrinsicsYamlConstructor() {
        var loaderOptions = new LoaderOptions();
        loaderOptions.setEnumCaseSensitive(false);
        loaderOptions.setMaxAliasesForCollections(50);
        loaderOptions.setNestingDepthLimit(50);
        loaderOptions.setCodePointLimit((int) DataSize.ofMegabytes(3).toBytes());
        return new BlueprintIntrinsicsYamlConstructor(loaderOptions);
    }

    @Bean
    public Yaml blueprintSnakeYaml(BlueprintIntrinsicsYamlConstructor blueprintIntrinsicsYamlConstructor) {
        return new Yaml(blueprintIntrinsicsYamlConstructor);
    }

}
