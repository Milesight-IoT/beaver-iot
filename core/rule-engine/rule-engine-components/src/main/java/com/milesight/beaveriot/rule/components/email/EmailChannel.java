package com.milesight.beaveriot.rule.components.email;

import java.util.List;

public interface EmailChannel {

    void send(String fromName, String fromAddress, List<String> to, String subject, String content);

}
