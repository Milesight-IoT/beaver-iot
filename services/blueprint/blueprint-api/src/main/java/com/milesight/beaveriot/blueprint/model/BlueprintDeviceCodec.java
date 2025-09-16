package com.milesight.beaveriot.blueprint.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.milesight.beaveriot.base.utils.StringUtils;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/8 16:36
 **/
@Data
public class BlueprintDeviceCodec {
    private String id;
    private Decoder decoder;
    private Encoder encoder;

    @Data
    public static class Decoder {
        private List<Codec> chain;
    }

    @Data
    public static class Encoder {
        private List<Codec> chain;
    }

    @Data
    public static class Codec {
        private String script;
        private String entry;
        private List<Argument> args;

        public boolean validate() {
            if (StringUtils.isEmpty(script)) {
                return false;
            }

            if (StringUtils.isEmpty(entry)) {
                return false;
            }

            return !CollectionUtils.isEmpty(args);
        }
    }

    @Data
    public static class Argument {
        private String id;
        @JsonProperty("is_payload")
        private boolean isPayload;
    }

    public boolean validate() {
        if (decoder == null) {
            return false;
        }

        if (encoder == null) {
            return false;
        }

        if (CollectionUtils.isEmpty(decoder.getChain())) {
            return false;
        }

        if (CollectionUtils.isEmpty(encoder.getChain())) {
            return false;
        }

        return decoder.getChain().stream().allMatch(Codec::validate) && encoder.getChain().stream().allMatch(Codec::validate);
    }
}