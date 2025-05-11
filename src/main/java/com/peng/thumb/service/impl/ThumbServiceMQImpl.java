package com.peng.thumb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.peng.thumb.constant.RedisLuaScriptConstant;
import com.peng.thumb.constant.ThumbConstant;
import com.peng.thumb.listener.thumb.msg.ThumbEvent;
import com.peng.thumb.manager.cache.CacheManager;
import com.peng.thumb.mapper.ThumbMapper;
import com.peng.thumb.model.dto.thumb.DoThumbRequest;
import com.peng.thumb.model.entity.Blog;
import com.peng.thumb.model.entity.Thumb;
import com.peng.thumb.model.entity.User;
import com.peng.thumb.model.enums.LuaStatusEnum;
import com.peng.thumb.service.BlogService;
import com.peng.thumb.service.ThumbService;
import com.peng.thumb.service.UserService;
import com.peng.thumb.util.RedisKeyUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;


/**
 * @author pjw
 * @description 针对表【thumb】的数据库操作Service实现
 * @createDate 2025-04-27 01:38:39
 */
@Service("thumbService")
@Slf4j
@RequiredArgsConstructor
public class ThumbServiceMQImpl extends ServiceImpl<ThumbMapper, Thumb>
        implements ThumbService {

    private final UserService userService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final PulsarTemplate pulsarTemplate;

    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (ObjectUtils.isEmpty(doThumbRequest) || ObjectUtils.isEmpty(doThumbRequest.getBlogId())) {
            throw new RuntimeException("请求参数为空");
        }

        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        Long blogId = doThumbRequest.getBlogId();
        String userThumbKey = RedisKeyUtil.getUserThumbKey(userId);
        Long result = redisTemplate.execute(
                RedisLuaScriptConstant.THUMB_SCRIPT_MQ,
                List.of(userThumbKey),
                blogId
        );
        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new RuntimeException("用户已点赞");
        }

        ThumbEvent thumbEvent = ThumbEvent.builder()
                .userId(userId)
                .blogId(blogId)
                .type(ThumbEvent.EventType.INCR)
                .eventTime(LocalDateTime.now())
                .build();
        pulsarTemplate.sendAsync("thumb-topic", thumbEvent).exceptionally(ex -> {
            redisTemplate.opsForHash().delete(userThumbKey, blogId.toString(), true);
            log.error("点赞事件发送失败：userId={}, blogId={}", userId, blogId, ex);
            return null;
        });

        return true;
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (ObjectUtils.isEmpty(doThumbRequest) || ObjectUtils.isEmpty(doThumbRequest.getBlogId())) {
            throw new RuntimeException("请求参数为空");
        }
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        Long blogId = doThumbRequest.getBlogId();
        String userThumbKey = RedisKeyUtil.getUserThumbKey(userId);
        Long result = redisTemplate.execute(
                RedisLuaScriptConstant.UNTHUMB_SCRIPT_MQ,
                List.of(userThumbKey),
                blogId
        );
        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new RuntimeException("用户未点赞");
        }
        ThumbEvent thumbEvent = ThumbEvent.builder()
                .userId(userId)
                .blogId(blogId)
                .type(ThumbEvent.EventType.DECR)
                .eventTime(LocalDateTime.now())
                .build();
        pulsarTemplate.sendAsync("thumb-topic", thumbEvent).exceptionally(ex -> {
            redisTemplate.opsForHash().put(userThumbKey, blogId.toString(), true);
            log.error("取消点赞事件发送失败：userId={}, blogId={}", userId, blogId, ex);
            return null;
        });
        return true;
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        return redisTemplate.opsForHash().hasKey(RedisKeyUtil.getUserThumbKey(userId), blogId.toString());
    }
}




