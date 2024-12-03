package com.milesight.beaveriot.base.response;


import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.GenericPageResult;
import com.milesight.beaveriot.base.tracer.TraceIdProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author leon
 */
public class ResponseBuilder {

    private ResponseBuilder() {
    }

    /**
     * default ResponseBody successful response status
     */
    public static final String DEFAULT_RESPONSE_STATUS_SUCCESS = "Success";

    /**
     * Default ResponseBody failure response status
     */
    public static final String DEFAULT_RESPONSE_STATUS_FAILED = "Failed";

    public static <T> ResponseBody<T> success() {
        return success(null);
    }

    public static <T> ResponseBody<T> success(T data) {
        Object result = (data instanceof PageImpl<?> pageImpl) ? GenericPageResult.of(pageImpl) : data;
        return new ResponseBody()
                .data(result)
                .requestId(TraceIdProvider.traceIdProvider().getTraceId())
                .onSuccess();
    }

    public static ResponseBody<Object> fail(ErrorCodeSpec errorCodeSpec, Object data) {
        return fail(errorCodeSpec.getErrorCode(), errorCodeSpec.getErrorMessage(), errorCodeSpec.getDetailMessage(), data);
    }

    public static ResponseBody<Object> fail(ErrorCodeSpec errorCodeSpec, String detailMessage, Object data) {
        return fail(errorCodeSpec.getErrorCode(), errorCodeSpec.getErrorMessage(), detailMessage, data);
    }

    public static ResponseBody<Object> fail(ErrorCodeSpec errorCodeSpec) {
        return fail(errorCodeSpec.getErrorCode(), errorCodeSpec.getErrorMessage(), errorCodeSpec.getDetailMessage(), null);
    }

    public static ResponseBody<Object> fail(ErrorCodeSpec errorCodeSpec, String detailMessage) {
        return fail(errorCodeSpec.getErrorCode(), errorCodeSpec.getErrorMessage(), detailMessage, null);
    }

    public static ResponseBody<Object> fail(String errorCode, String errorMsg) {
        return fail(errorCode, errorMsg, null, null);
    }

    public static ResponseBody<Object> fail(String errCode, String errorMessage, String detailMessage, Object data) {
        //If errorMessage is empty, use detailMessage
        if (ObjectUtils.isEmpty(errorMessage) && StringUtils.hasText(detailMessage)) {
            errorMessage = detailMessage;
            detailMessage = null;
        }
        return new ResponseBody<>()
                .data(data)
                .requestId(TraceIdProvider.traceIdProvider().getTraceId())
                .onFailed()
                .errorCode(errCode)
                .errorMessage(errorMessage)
                .detailMessage(detailMessage);
    }

    public static ResponseBody<Object> fail(ServiceException exception) {
        return fail(exception.getErrorCode(), exception.getErrorMessage(), exception.getDetailMessage(), exception.getArgs());
    }

}
