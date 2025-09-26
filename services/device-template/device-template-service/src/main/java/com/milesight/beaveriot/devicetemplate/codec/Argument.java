package com.milesight.beaveriot.devicetemplate.codec;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/9/8 9:35
 **/
@Data
public class Argument {
    private String id;
    private boolean isPayload;

    public static Argument of(String id) {
        return of(id, false);
    }

    public static Argument of(String id, boolean isPayload) {
        Argument argument = new Argument();
        argument.id = id;
        argument.isPayload = isPayload;
        return argument;
    }
}
