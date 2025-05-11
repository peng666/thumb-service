package com.peng.thumb.job;

import com.google.common.collect.Sets;
import com.peng.thumb.constant.ThumbConstant;
import com.peng.thumb.listener.thumb.msg.ThumbEvent;
import com.peng.thumb.model.entity.Thumb;
import com.peng.thumb.service.ThumbService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ThumbReconcileJob {

    @Resource
    private ThumbService thumbService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private PulsarTemplate<ThumbEvent> pulsarTemplate;

    @Scheduled(cron = "0 0 2 * * ?")
    public void run() {
        long startTime = System.currentTimeMillis();
        HashSet<Long> userIds = new HashSet<>();
        String pattern = ThumbConstant.USER_THUMB_KEY_PREFIX + "*";
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(1000).build());) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                Long userId = Long.valueOf(key.replace(ThumbConstant.USER_THUMB_KEY_PREFIX, ""));
                userIds.add(userId);
            }
        }

        userIds.forEach(userId -> {
            // 分别获取该用户的两边数据库的博客id
            Set<Long> redisBlogIds = redisTemplate.opsForHash().keys(ThumbConstant.USER_THUMB_KEY_PREFIX + userId)
                    .stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .collect(Collectors.toSet());
            Set<Long> mysqlBlogIds = Optional.ofNullable(thumbService.lambdaQuery()
                            .eq(Thumb::getUserId, userId)
                            .list())
                    .orElse(new ArrayList<>())
                    .stream()
                    .map(Thumb::getBlogId)
                    .collect(Collectors.toSet());

            // 计算差异（redis有mysql没有）
            Set<Long> diffBlogIds = Sets.difference(redisBlogIds, mysqlBlogIds);

            // 发送补偿事件
            sendCompensationEvent(userId, diffBlogIds);
        });
        log.info("对账任务完成, cost: {}ms", System.currentTimeMillis() - startTime);
    }

    // 发送补偿事件到消息队列pulsar
    private void sendCompensationEvent(Long userId, Set<Long> blogIds) {
        blogIds.forEach(blogId -> {
            ThumbEvent thumbEvent = new ThumbEvent(userId, blogId, ThumbEvent.EventType.INCR, LocalDateTime.now());
            pulsarTemplate.sendAsync("thumb-topic", thumbEvent)
                    .exceptionally(ex -> {
                        log.error("补偿事件发送失败： userId={}, blogId={}", userId, blogId, ex);
                        return null;
                    });
        });
    }
}
