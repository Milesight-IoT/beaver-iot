package com.milesight.beaveriot.devicetemplate.codec;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.milesight.beaveriot.base.utils.StringUtils;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/8 9:06
 **/
@Builder
@Data
public class CodecExecutor {
    public static final ObjectMapper JSON = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HostAccess HOST_ACCESS = HostAccess.newBuilder()
            .allowArrayAccess(true)
            .allowMapAccess(true)
            .allowListAccess(true)
            .build();
    private static final String LANGUAGE_ID = "js";
    private static final Engine ENGINE = Engine.newBuilder(LANGUAGE_ID)
            .option("engine.WarnInterpreterOnly", "false")
            .build();

    private String code;
    private String entry;
    private List<Argument> arguments;
    @Builder.Default
    private ReturnType returnType = ReturnType.JSON;

    private static Context buildCodeCtx() {
        return Context.newBuilder(LANGUAGE_ID)
                .allowHostAccess(HOST_ACCESS)
                .engine(ENGINE)
                .build();
    }

    public Object execute(Object data, Map<String, Object> argContext) {
        try (Context context = buildCodeCtx()) {
            context.eval(LANGUAGE_ID, code);
            Value binding = context.getBindings(LANGUAGE_ID);
            Value func = binding.getMember(entry);
            List<Object> args = buildArgs(context, data, argContext);
            return execute(func, args.toArray());
        }
    }

    @SneakyThrows
    private Object execute(Value function, Object... args) {
        if (returnType == ReturnType.JSON) {
            String jsonString = mapper.writeValueAsString(function.execute(args).as(Map.class));
            return JSON.readTree(jsonString);
        } else if (returnType == ReturnType.BYTES) {
            Integer[] intArray = function.execute(args).as(Integer[].class);
            byte[] byteArray = new byte[intArray.length];
            for (int i = 0; i < intArray.length; i++) {
                byteArray[i] = intArray[i].byteValue();
            }
            return byteArray;
        } else {
            return null;
        }
    }

    private List<Object> buildArgs(Context context, Object data, Map<String, Object> argContext) {
        if (CollectionUtils.isEmpty(arguments)) {
            throw new IllegalArgumentException("Arguments cannot be empty");
        }

        List<Object> args = new ArrayList<>();
        int payloadCount = 0;
        for (Argument argument : arguments) {
            if (StringUtils.isEmpty(argument.getId())) {
                throw new IllegalArgumentException("Argument id cannot be empty");
            }

            if (argument.isPayload()) {
                args.add(convertArg(context, data));
                payloadCount++;
            } else {
                if (argContext == null) {
                    args.add(null);
                } else {
                    if (!argContext.containsKey(argument.getId())) {
                        throw new IllegalArgumentException("ArgContext cannot find id: " + argument.getId());
                    }
                    args.add(convertArg(context, argContext.get(argument.getId())));
                }
            }
        }

        if (payloadCount != 1) {
            throw new IllegalArgumentException("Exactly one argument must be marked as payload, but found: " + payloadCount);
        }

        return args;
    }

    private Object convertArg(Context context, Object object) {
        if (object instanceof JsonNode jsonNodeObject) {
            return convertToJSObject(context, jsonNodeObject);
        } else if (object instanceof byte[] byteArray) {
            int[] intArray = new int[byteArray.length];
            for (int i = 0; i < byteArray.length; i++) {
                intArray[i] = byteArray[i] & 0xFF;
            }
            return intArray;
        } else {
            return object;
        }
    }

    private Value convertToJSObject(Context context, JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            Value jsObject = context.eval("js", "({})");
            jsonNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldValue = jsonNode.get(fieldName);
                jsObject.putMember(fieldName, convertToJSObject(context, fieldValue));
            });
            return jsObject;
        } else if (jsonNode.isArray()) {
            Value jsArray = context.eval("js", "([])");
            for (int i = 0; i < jsonNode.size(); i++) {
                jsArray.setArrayElement(i, convertToJSObject(context, jsonNode.get(i)));
            }
            return jsArray;
        } else if (jsonNode.isTextual()) {
            return context.asValue(jsonNode.asText());
        } else if (jsonNode.isNumber()) {
            return context.asValue(jsonNode.numberValue());
        } else if (jsonNode.isBoolean()) {
            return context.asValue(jsonNode.asBoolean());
        } else if (jsonNode.isNull()) {
            return context.eval("js", "null");
        } else {
            return context.asValue(jsonNode.toString());
        }
    }
}