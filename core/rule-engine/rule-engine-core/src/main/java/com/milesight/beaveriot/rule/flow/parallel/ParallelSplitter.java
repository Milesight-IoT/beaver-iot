package com.milesight.beaveriot.rule.flow.parallel;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leon
 */
@Data
public class ParallelSplitter {

    public List<Object> split(Object payload, int parallelSize) {
        List<Object> payloads = new ArrayList<>();
        for (int i = 0; i < parallelSize; i++) {
            payloads.add(payload);
        }
        return payloads;
    }
}
