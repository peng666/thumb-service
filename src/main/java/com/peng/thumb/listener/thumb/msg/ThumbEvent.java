package com.peng.thumb.listener.thumb.msg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThumbEvent implements Serializable {
    /**
     * 点赞的用户id
     */
    private Long userId;

    /**
     * 点赞的博客id
     */
    private Long blogId;

    /**
     * 事件的类型
     */
    private EventType type;

    /**
     * 事件的发生时间
     */
    private LocalDateTime eventTime;

    /**
     * 事件类型的枚举
     */
    public enum EventType {
        /**
         * 点赞
         */
        INCR,

        /**
         * 取消点赞
         */
        DECR
    }
}
