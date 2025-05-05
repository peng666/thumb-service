package com.peng.thumb.manager.cache;

import cn.hutool.core.util.ObjectUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CacheManager {
    private TopK hotKeyDetector;
    private Cache<String, Object> localCache;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Bean
    public TopK getHotKeyDetector() {
        hotKeyDetector = new HeavyKeeper(100, 100000, 5, 0.92, 10);
        return hotKeyDetector;
    }

    @Bean
    public Cache<String, Object> localCache() {
        return localCache = Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(5, TimeUnit.MINUTES).build();
    }

    private String buildCacheKey(String hashKey, String key) {
        return hashKey + ":" + key;
    }

    public Object get(String hashKey, String key) {
        String compositeKey = buildCacheKey(hashKey, key);

        Object value = localCache.getIfPresent(compositeKey);
        if (ObjectUtil.isNotEmpty(value)) {
            log.info("本地缓存获取到数据： {} = {}", compositeKey, value);
            hotKeyDetector.add(key, 1);
            return value;
        }

        Object redisValue = redisTemplate.opsForHash().get(hashKey, key);
        if (ObjectUtil.isEmpty(redisValue)) {
            return null;
        }
        AddResult addResult = hotKeyDetector.add(key, 1);
        if (addResult.isHotKey()) { // 热key，放入本地缓存
            localCache.put(compositeKey, redisValue);
        }
        return redisValue;
    }

    public void putIfPresent(String hashKey, String key, Object value) {
        String compositeKey = buildCacheKey(hashKey, key);
        Object object = localCache.getIfPresent(compositeKey);
        if (object == null) {
            return;
        }
        localCache.put(compositeKey, value);
    }

    @Scheduled(fixedRate = 20, timeUnit = TimeUnit.SECONDS)
    public void cleanHotkey() {
        hotKeyDetector.fading();
    }
}
