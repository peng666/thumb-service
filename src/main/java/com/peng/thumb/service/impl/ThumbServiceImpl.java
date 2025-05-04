package com.peng.thumb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.peng.thumb.model.dto.thumb.DoThumbRequest;
import com.peng.thumb.model.entity.Blog;
import com.peng.thumb.model.entity.Thumb;
import com.peng.thumb.model.entity.User;
import com.peng.thumb.service.BlogService;
import com.peng.thumb.service.ThumbService;
import com.peng.thumb.mapper.ThumbMapper;
import com.peng.thumb.service.UserService;
import com.peng.thumb.util.RedisKeyUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;


/**
 * @author pjw
 * @description 针对表【thumb】的数据库操作Service实现
 * @createDate 2025-04-27 01:38:39
 */
@Service("thumbServiceDB")
@Slf4j
@RequiredArgsConstructor
public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb>
        implements ThumbService {


    private final UserService userService;

    private final BlogService blogService;

    private final TransactionTemplate transactionTemplate;

    private final RedisTemplate redisTemplate;


    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (ObjectUtils.isEmpty(doThumbRequest) || ObjectUtils.isEmpty(request)) {
            throw new RuntimeException("请求参数为空");
        }

        User loginUser = userService.getLoginUser(request);

        // 加锁
        synchronized (loginUser.getId().toString().intern()) {
            return transactionTemplate.execute(status -> {
                Long blogId = doThumbRequest.getBlogId();
                boolean exists = this.hasThumb(blogId, loginUser.getId());
                if (exists) {
                    throw new RuntimeException("用户已点赞");
                }
                boolean update = blogService.lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .setSql("thumbCount=thumbCount+1")
                        .update();

                Thumb thumb = new Thumb();
                thumb.setBlogId(blogId);
                thumb.setUserId(loginUser.getId());
                boolean success = update && this.save(thumb);
                if (success) {
                    redisTemplate.opsForHash().put(RedisKeyUtil.getUserThumbKey(loginUser.getId()), blogId.toString(), thumb.getId());
                }
                return success;
            });
        }
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (ObjectUtils.isEmpty(doThumbRequest) || ObjectUtils.isEmpty(request)) {
            throw new RuntimeException("请求参数为空");
        }

        User loginUser = userService.getLoginUser(request);

        synchronized (loginUser.getId().toString().intern()) {
            return transactionTemplate.execute(status -> {
                Long blogId = doThumbRequest.getBlogId();

                Object thumbIdObj = redisTemplate.opsForHash().get(RedisKeyUtil.getUserThumbKey(loginUser.getId()), blogId.toString());
                if (ObjectUtils.isEmpty(thumbIdObj)) {
                    throw new RuntimeException("用户未点赞");
                }
                Long thumbId = Long.valueOf(thumbIdObj.toString());


                boolean update = blogService.lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .setSql("thumbCount=thumbCount-1")
                        .update();

                boolean success = update && this.removeById(thumbId);
                if (success) {
                    redisTemplate.opsForHash().delete(RedisKeyUtil.getUserThumbKey(loginUser.getId()), blogId.toString());
                }
                return success;
            });
        }
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        return redisTemplate.opsForHash().hasKey(RedisKeyUtil.getUserThumbKey(userId), blogId.toString());
    }
}




