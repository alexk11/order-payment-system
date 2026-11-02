package dev.alexk.async;

import dev.alexk.utils.EnumUtils;


public enum TaskStatus implements EnumUtils.IntEnum {

    NEW(1),
    IN_PROGRESS(2),
    SUCCEEDED(3),
    FAILED_RETRYABLE(4),
    FAILED_NON_RETRYABLE(5);

    private final int value;

    TaskStatus(Integer value) {
        this.value = value;
    }

    @Override
    public int getCode() {
        return value;
    }

}
