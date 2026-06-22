package com.example.oa.common.enums;

import lombok.Getter;

@Getter
public enum StatusEnum {
    DISABLED(0),
    ENABLED(1);

    private final int value;

    StatusEnum(int value) {
        this.value = value;
    }
}
