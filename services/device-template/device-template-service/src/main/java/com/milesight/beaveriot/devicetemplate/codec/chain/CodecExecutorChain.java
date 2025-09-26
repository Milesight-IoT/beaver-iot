package com.milesight.beaveriot.devicetemplate.codec.chain;

import com.milesight.beaveriot.devicetemplate.codec.CodecExecutor;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/8 13:18
 **/
@Data
public abstract class CodecExecutorChain {
    private List<CodecExecutor> executors = new ArrayList<>();

    public void addExecutor(CodecExecutor executor) {
        executors.add(executor);
    }

    public Object execute(Object data, Map<String, Object> argContext) {
        if (CollectionUtils.isEmpty(executors)) {
            return null;
        }

        initLastExecutor(executors.get(executors.size() - 1));

        for (CodecExecutor executor : executors) {
            data = executor.execute(data, argContext);
        }
        return data;
    }

    protected abstract void initLastExecutor(CodecExecutor executor);
}
