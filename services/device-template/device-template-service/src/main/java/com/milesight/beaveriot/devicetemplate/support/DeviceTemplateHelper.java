package com.milesight.beaveriot.devicetemplate.support;

import java.text.MessageFormat;

/**
 * author: Luxb
 * create: 2025/9/9 13:32
 **/
public class DeviceTemplateHelper {
    private static final String TEMPLATE_IDENTIFIER_FORMAT = "{0}@{1}:{2}@{3}";
    private static final String TEMPLATE_NAME_FORMAT = "{0} ({1})";
    public static String getTemplateIdentifier(Long blueprintLibraryId, String blueprintLibraryVersion, String vendor, String model) {
        return MessageFormat.format(TEMPLATE_IDENTIFIER_FORMAT, String.valueOf(blueprintLibraryId), blueprintLibraryVersion.replace(".", "_"), model, vendor);
    }

    public static String getTemplateName(String vendor, String model) {
        return MessageFormat.format(TEMPLATE_NAME_FORMAT, model, vendor);
    }
}