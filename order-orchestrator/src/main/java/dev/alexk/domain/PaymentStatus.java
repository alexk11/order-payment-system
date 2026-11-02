package dev.alexk.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import dev.alexk.utils.EnumUtils;


public enum PaymentStatus implements EnumUtils.IntEnum, EnumUtils.StringEnum {

    NEW(1, "NEW_ORDER"),
    AUTHORIZATION_FAILED(2, "AUTHORIZATION_FAILED"),
    PRICE_CHANGED_FAILED(3, "PRICE_CHANGED_FAILED"),
    PENDING(4, "PENDING"),
    SUCCESS_PAID(5, "SUCCESS_PAID"),
    CAPTURE_FAILED(6, "CAPTURE_FAILED");

    private final int code;
    private final String stringValue;

    PaymentStatus(int code, String stringValue) {
        this.code = code;
        this.stringValue = stringValue;
    }

    @JsonValue
    @Override
    public String getValue() {
        return stringValue;
    }

    @JsonCreator
    public static PaymentStatus fromStringValue(int code) {
        return EnumUtils.fromCode(PaymentStatus.class, code);
    }

    public static PaymentStatus fromCode(int code) {
        return EnumUtils.fromCode(PaymentStatus.class, code);
    }

    @Override
    public int getCode() {
        return code;
    }
}
