package com.milesight.beaveriot.rule.components.httpin;

import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.enums.DataTypeEnums;
import com.milesight.beaveriot.rule.model.OutputVariablesSettings;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.springframework.web.util.UriTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * HttpInComponent class.
 *
 * @author simon
 * @date 2025/4/17
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
@RuleNode(
        value = "httpIn",
        description = "Listen for incoming HTTP requests to trigger a running workflow.",
        type = RuleNodeType.ENTRY,
        testable = false
)
@UriEndpoint(firstVersion = "4.4.3",
        scheme = "httpIn",
        title = "HTTP in",
        syntax = "httpIn:flowId",
        remote = false,
        consumerOnly = true,
        category = {Category.MESSAGING}
)
public class HttpInEndpoint extends DefaultEndpoint {
    @Metadata(required = true, autowired = true)
    private String flowId;

    @Metadata(required = true, autowired = true)
    private String tenantId;

    @UriParam
    @Metadata(required = true, autowired = true)
    private String nodeId;

    @Metadata(required = true, autowired = true)
    private Long credentialsId;

    @Metadata(required = true, autowired = true)
    private UriTemplate urlTemplate;

    @Metadata(required = true)
    @UriParamExtension(uiComponentGroup = "Setting")
    @UriParam(displayName = "HTTP Method", enums = "POST,GET,PUT,DELETE")
    private String method;

    @Metadata(required = true)
    @UriParamExtension(uiComponentGroup = "Setting")
    @UriParam(displayName = "Url")
    private String url;

    @OutputArguments(displayName = "Output Variables")
    @UriParam(displayName = "Output Variables")
    @Metadata(required = true, autowired = true)
    private List<OutputVariablesSettings> messageOut = new ArrayList<>();

    public HttpInEndpoint() {
        super();
    }

    public HttpInEndpoint(String uri, String flowId, HttpInComponent component) {
        super(uri, component);
        this.flowId = flowId;
        assert TenantContext.getTenantId() != null;
        this.setTenantId(TenantContext.getTenantId());
        this.credentialsId = component.credentialsServiceProvider.getOrCreateCredentials(CredentialsType.HTTP).getId();
    }

    @Override
    public Producer createProducer() {
        return null;
    }

    private String standardizeUrl(String inputUrl) {
        if (inputUrl.startsWith("/")) {
            return inputUrl.substring(1);
        }

        return inputUrl;
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        urlTemplate = new UriTemplate(standardizeUrl(this.url));
        messageOut = new ArrayList<>();
        messageOut.add(new OutputVariablesSettings(DataTypeEnums.STRING, HttpInConstants.OUT_HEADER_NAME));
        messageOut.add(new OutputVariablesSettings(DataTypeEnums.STRING, HttpInConstants.OUT_BODY_NAME));
        messageOut.add(new OutputVariablesSettings(DataTypeEnums.STRING, HttpInConstants.OUT_URL_NAME));
        messageOut.add(new OutputVariablesSettings(DataTypeEnums.STRING, HttpInConstants.OUT_PARAM_NAME));
        urlTemplate.getVariableNames().forEach(variableName -> messageOut.add(new OutputVariablesSettings(DataTypeEnums.STRING, HttpInConstants.OUT_PATH_PARAM_NAME + "." + variableName)));
        return new HttpInConsumer(this, processor);
    }
}
