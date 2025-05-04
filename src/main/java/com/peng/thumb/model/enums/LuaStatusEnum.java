package com.peng.thumb.model.enums;

import lombok.Getter;

@Getter
public enum LuaStatusEnum {

    SUCCESS(1L),    // 成功
    FAIL(-1L),      // 失败
    ;
    private final Long value;

    LuaStatusEnum(Long value) {
        this.value = value;
    }
}
