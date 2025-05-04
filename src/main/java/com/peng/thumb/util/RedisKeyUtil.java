package com.peng.thumb.util;

import com.peng.thumb.constant.ThumbConstant;

public class RedisKeyUtil {

    public static String getUserThumbKey(Long userId) {
        return ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
    }

    public static String getTempThumbKey(String time) {
        return String.format(ThumbConstant.TEMP_THUMB_KEY_PREFIX, time);
    }
}
