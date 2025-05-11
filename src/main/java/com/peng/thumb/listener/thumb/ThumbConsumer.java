package com.peng.thumb.listener.thumb;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peng.thumb.listener.thumb.msg.ThumbEvent;
import com.peng.thumb.mapper.BlogMapper;
import com.peng.thumb.model.entity.Thumb;
import com.peng.thumb.service.ThumbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;
import org.apache.tomcat.util.http.parser.TE;
import org.checkerframework.checker.units.qual.A;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbConsumer {

    private final BlogMapper blogMapper;

    private final ThumbService thumbService;

    @PulsarListener(
            subscriptionName = "thumb-subscription",
            topics = "thumb-topic",
            schemaType = SchemaType.JSON,
            batch = true,
            consumerCustomizer = "thumbConsumerConfig",
            negativeAckRedeliveryBackoff = "negativeAckRedeliveryBackoff",
            ackTimeoutRedeliveryBackoff = "ackTimeoutRedeliveryBackoff",
            subscriptionType = SubscriptionType.Shared,
            deadLetterPolicy = "deadLetterPolicy"
    )
    @Transactional(rollbackFor = Exception.class)
    public void processBath(List<Message<ThumbEvent>> messages) {
        log.info("ThumbConsumer processBatch: {}", messages.size());
        // 打印每条 message 的关键信息
        messages.forEach(message -> {
            log.info("Message ID: {}, Publish Time: {}, Message Body: {}",
                    message.getMessageId(),
                    message.getPublishTime(),
                    message.getValue());
        });

        Map<Long, Long> countMap = new ConcurrentHashMap<>();
        List<Thumb> thumbs = new ArrayList<>();

        // 并行处理消息
        LambdaQueryWrapper<Thumb> wrapper = new LambdaQueryWrapper<>();
        AtomicReference<Boolean> needRemove = new AtomicReference<>(false);

        List<ThumbEvent> events = messages.stream().map(Message::getValue).filter(Objects::nonNull).toList();

        // 根据(userId, blogId)分组,获取每个分组的最新消息
        Map<Pair<Long, Long>, ThumbEvent> latestEvents = events.stream()
                .collect(Collectors.groupingBy(
                        e -> Pair.of(e.getUserId(), e.getBlogId()),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    list.sort(Comparator.comparing(ThumbEvent::getEventTime));
                                    if (list.size() % 2 == 0) {
                                        return null;
                                    }
                                    return list.get(list.size() - 1);
                                }
                        )
                ));


        latestEvents.forEach((userBlogPair, event) -> {
            log.info("userBlogPair: {}, event: {}", userBlogPair, event);
            if (ObjectUtils.isEmpty(event)) {
                return;
            }
            ThumbEvent.EventType finalAction = event.getType();
            if (finalAction == ThumbEvent.EventType.INCR) {
                countMap.merge(event.getBlogId(), 1L, Long::sum);
                Thumb thumb = new Thumb();
                thumb.setUserId(event.getUserId());
                thumb.setBlogId(event.getBlogId());
                thumbs.add(thumb);
            } else {
                needRemove.set(true);
                wrapper.or().eq(Thumb::getUserId, event.getUserId()).eq(Thumb::getBlogId, event.getBlogId());
                countMap.merge(event.getBlogId(), -1L, Long::sum);
            }
        });

        log.info("准备更新数据库： {} {}", countMap, thumbs);

        // 批量更新数据库
        if (needRemove.get()) {
            thumbService.remove(wrapper);
        }
        batchUpdateBlog(countMap);
        batchInsertThumb(thumbs);
    }

    private void batchInsertThumb(List<Thumb> thumbs) {
        if (!thumbs.isEmpty()) {
            thumbService.saveBatch(thumbs, 500);
        }
    }

    private void batchUpdateBlog(Map<Long, Long> countMap) {
        if (!countMap.isEmpty()) {
            blogMapper.BatchUpdateThumbCount(countMap);
        }
    }


    @PulsarListener(topics = "thumb-dlq-topic")
    public void consumerDlq(Message<ThumbEvent> message) {
        MessageId messageId = message.getMessageId();
        log.info("dlq message: {}", messageId);
        log.info("[死信队列] 消息: {} 内容: {}。已通知相关人员", messageId, message.getValue());
    }
}
