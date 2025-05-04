package com.peng.thumb.model.enums;

import lombok.Getter;

/**
 * 点赞类型
 */
@Getter
public enum ThumbTypeEnum {
    INCR(1),//   点赞
    DECR(-1),//   取消点赞
    NON(0), //  不发生改变
    ;

    private final int value;

    ThumbTypeEnum(int value) {
        this.value = value;
    }
}
