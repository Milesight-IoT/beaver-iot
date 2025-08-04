package com.milesight.beaveriot.base.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;


@Getter
@RequiredArgsConstructor
public enum ValidationErrorCode implements ErrorCodeSpec {
    PARAMETER_PATTERN_INVALID("Pattern"),
    PARAMETER_SIZE_INVALID("Size"),
    PARAMETER_MUST_BE_A_EMAIL_ADDRESS("Email"),
    PARAMETER_LESS_THAN_MIN_VALUE("Min"),
    PARAMETER_GREATER_THAN_MAX_VALUE("Max"),
    PARAMETER_MUST_NOT_BE_NULL("NotNull"),
    PARAMETER_MUST_NOT_BE_EMPTY("NotEmpty"),
    PARAMETER_MUST_NOT_BE_BLANK("NotBlank"),
    ;

    private static final Map<String, ValidationErrorCode> VALIDATION_CODE_TO_ERROR = Arrays.stream(ValidationErrorCode.values())
            .collect(Collectors.toMap(ValidationErrorCode::getValidationCode, v -> v, (a, b) -> a));

    @Nullable
    public static ValidationErrorCode fromValidationCode(String validationCode) {
        if (validationCode == null || validationCode.isEmpty()) {
            return null;
        }
        return VALIDATION_CODE_TO_ERROR.get(validationCode);
    }

    private final int status = HttpStatus.BAD_REQUEST.value();
    private final String errorCode = name().toLowerCase();
    private final String validationCode;
    private String errorMessage;
    private String detailMessage;

    @Override
    public String toString() {
        return name();
    }
}
