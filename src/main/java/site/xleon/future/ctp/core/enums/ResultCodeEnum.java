package site.xleon.future.ctp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ResultCodeEnum {
    SUCCESS(1, "success"),
    WARN(0, "warn"),
    ERROR(-1, "error"),
    TOKEN_EXPIRY(-999, "token expiry");

    @Getter
    private final Integer value;
    @Getter
    private final String label;
}
