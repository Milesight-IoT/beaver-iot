package com.milesight.beaveriot.coalescer;

import java.time.Duration;

/**
 * RequestCoalescerConstants class.
 *
 * @author simon
 * @date 2025/12/10
 */
public class RequestCoalescerConstants {
    private RequestCoalescerConstants() {}

    public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
}
