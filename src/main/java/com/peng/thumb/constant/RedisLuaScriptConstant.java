package com.peng.thumb.constant;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

public class RedisLuaScriptConstant {

    public static final RedisScript<Long> THUMB_SCRIPT = new DefaultRedisScript<>("""
            local tempThumbKey=KEYS[1] -- 临时计数键
            local userThumbKey=KEYS[2] -- 用户点赞状态键
            local userId = ARGV[1]      -- 用户ID
            local blogId = ARGV[2]      -- 博客ID
                        
            -- 1.检是否已经点赞
            if redis.call('HEXISTS', userThumbKey, blogId) == 1 then
                return -1
            end
                        
            -- 2.获取临时计数键的值
            local hashKey = userId .. ':' .. blogId
            local oldNumber = tonumber(redis.call('HGET', tempThumbKey, hashKey) or 0)
                        
            -- 3. 计算新值
            local newNumber = oldNumber + 1
                       
            -- 4. 原子性更新
            redis.call('HSET', tempThumbKey, hashKey, newNumber)
            redis.call('HSET', userThumbKey, blogId, 1) 
                        
            return 1  -- 返回1表示成功                        
            """, Long.class
    );

    public static final RedisScript<Long> UNTHUMB_SCRIPT = new DefaultRedisScript<>("""
            local tempThumbKey=KEYS[1] -- 临时计数键
            local userThumbKey=KEYS[2] -- 用户点赞状态键
            local userId = ARGV[1]      -- 用户ID
            local blogId = ARGV[2]      -- 博客ID

            -- 1.检是否已经点赞
            if redis.call('HEXISTS', userThumbKey, blogId) ~= 1 then
                return -1
            end
            -- 2.获取临时计数键的值
            local hashKey = userId.. ':'.. blogId
            local oldNumber = tonumber(redis.call('HGET', tempThumbKey, hashKey) or 0)
            -- 3. 计算新值
            local newNumber = oldNumber - 1
            -- 4. 原子性更新
            redis.call('HSET', tempThumbKey, hashKey, newNumber)
            redis.call('HDEL', userThumbKey, blogId)
            return 1  -- 返回1表示成功
            """, Long.class
    );

    /**
     * 点赞Lua脚本 用于MQ消费的脚本
     * keys[1] 用户点赞状态键
     * ARGV[1] 博客ID
     */
    public static final RedisScript<Long> THUMB_SCRIPT_MQ = new DefaultRedisScript<>("""
            local userThumbKey=KEYS[1] -- 用户点赞状态键
            local blogId = ARGV[1]      -- 博客ID
                        
            -- 1.检是否已经点赞
            if redis.call('HEXISTS', userThumbKey, blogId) == 1 then
                return -1
            end

            -- 2. 原子性更新，添加点赞记录
            redis.call('HSET', userThumbKey, blogId, 1)
            return 1  -- 返回1表示成功
            """, Long.class);

    /**
     * 取消点赞Lua脚本 用于MQ消费的脚本
     * keys[1] 用户点赞状态键
     * ARGV[1] 博客ID
     */
    public static final RedisScript<Long> UNTHUMB_SCRIPT_MQ = new DefaultRedisScript<>("""
            local userThumbKey=KEYS[1] -- 用户点赞状态键
            local blogId = ARGV[1]      -- 博客ID
            -- 1.检是否已经点赞
            if redis.call('HEXISTS', userThumbKey, blogId) ~= 1 then
                return -1
            end
            -- 2. 原子性更新，删除点赞记录
            redis.call('HDEL', userThumbKey, blogId)
            return 1  -- 返回1表示成功
            """, Long.class);
}
