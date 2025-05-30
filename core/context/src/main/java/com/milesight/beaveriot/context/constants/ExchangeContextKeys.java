package com.milesight.beaveriot.context.constants;

import com.milesight.beaveriot.rule.constants.ExchangeHeaders;

/**
 * @author leon
 */
public class ExchangeContextKeys {

    private ExchangeContextKeys() {
    }

    /**
     * if the key is not found, ignore it
     */
    public static final String EXCHANGE_IGNORE_INVALID_KEY = "_exchangeIgnoreInvalidKey";

    /**
     * the key of the entities for exchange
     */
    public static final String EXCHANGE_ENTITIES = "_exchangeEntities";

    /**
     * if the exchange is sync call
     */
    public static final String EXCHANGE_SYNC_CALL = "_exchangeSyncCall";

    /**
     * the key of the exchange event type
     */
    public static final String EXCHANGE_EVENT_TYPE = "_exchangeEventType";

    /**
     * the key of the exchange source flow id
     */
    public static final String SOURCE_FLOW_ID = ExchangeHeaders.EXCHANGE_FLOW_ID;
    /**
     * the key of the exchange source user id
     */
    public static final String SOURCE_USER = "_sourceUser";
    /**
     * the key of exchange source tenant id
     */
    public static final String SOURCE_TENANT_ID = "_sourceTenantId";

    /**
     * the key of device name on add
     */
    public static final String DEVICE_NAME_ON_ADD = "_deviceNameOnAdd";

    /**
     * the key of device template id on add
     */
    public static final String DEVICE_TEMPLATE_ID_ON_ADD = "_deviceTemplateIdOnAdd";

    /**
     * the key of device on delete
     */
    public static final String DEVICE_ON_DELETE = "_deviceOnDelete";

    /**
     * the key of device template name on add
     */
    public static final String DEVICE_TEMPLATE_NAME_ON_ADD = "_deviceTemplateNameOnAdd";

    /**
     * the key of device template content on add
     */
    public static final String DEVICE_TEMPLATE_CONTENT_ON_ADD = "_deviceTemplateContentOnAdd";

    /**
     * the key of device template description on add
     */
    public static final String DEVICE_TEMPLATE_DESCRIPTION_ON_ADD = "_deviceTemplateDescriptionOnAdd";

    /**
     * the key of device template name on update
     */
    public static final String DEVICE_TEMPLATE_NAME_ON_UPDATE = "_deviceTemplateNameOnUpdate";

    /**
     * the key of device template content on update
     */
    public static final String DEVICE_TEMPLATE_CONTENT_ON_UPDATE = "_deviceTemplateContentOnUpdate";

    /**
     * the key of device template description on update
     */
    public static final String DEVICE_TEMPLATE_DESCRIPTION_ON_UPDATE = "_deviceTemplateDescriptionOnUpdate";

    /**
     * the key of device template id on update
     */
    public static final String DEVICE_TEMPLATE_ID_ON_UPDATE = "_deviceTemplateIdOnUpdate";

    /**
     * the key of device on delete
     */
    public static final String DEVICE_TEMPLATE_ON_DELETE = "_deviceTemplateOnDelete";
}
