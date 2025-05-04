package com.peng.thumb.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.peng.thumb.constant.RedisLuaScriptConstant;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * @author pjw
 * @description 针对表【thumb】的数据库操作Service实现
 * @createDate 2025-04-27 01:38:39
 */
@Service("thumbService")
@Slf4j
@RequiredArgsConstructor
public class ThumbServiceRedisImpl extends ServiceImpl<ThumbMapper, Thumb>
        implements ThumbService {


    private final UserService userService;

    private final RedisTemplate<String, Object> redisTemplate;


    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (ObjectUtils.isEmpty(doThumbRequest) || ObjectUtils.isEmpty(request)) {
            throw new RuntimeException("请求参数为空");
        }

        User loginUser = userService.getLoginUser(request);
        Long blogId = doThumbRequest.getBlogId();
        String timeSlice = getTimeSlice();

        String tempThumbKey = RedisKeyUtil.getTempThumbKey(timeSlice);
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUser.getId());

        Long result = redisTemplate.execute(
                RedisLuaScriptConstant.THUMB_SCRIPT,
                Arrays.asList(tempThumbKey, userThumbKey),
                loginUser.getId(),
                blogId
        );

        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new RuntimeException("用户已点赞");
        }
        return LuaStatusEnum.SUCCESS.getValue() == result;
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (ObjectUtils.isEmpty(doThumbRequest) || ObjectUtils.isEmpty(request)) {
            throw new RuntimeException("请求参数为空");
        }

        User loginUser = userService.getLoginUser(request);
        Long blogId = doThumbRequest.getBlogId();
        String timeSlice = getTimeSlice();
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(timeSlice);
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUser.getId());
        Long result = redisTemplate.execute(
                RedisLuaScriptConstant.UNTHUMB_SCRIPT,
                Arrays.asList(tempThumbKey, userThumbKey),
                loginUser.getId(),
                blogId
        );
        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new RuntimeException("用户未点赞");
        }
        return LuaStatusEnum.SUCCESS.getValue() == result;
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        return redisTemplate.opsForHash().hasKey(RedisKeyUtil.getUserThumbKey(userId), blogId.toString());
    }

    private String getTimeSlice() {
        DateTime nowDate = DateUtil.date();
        return DateUtil.format(nowDate, "HH:mm:") + (DateUtil.second(nowDate) / 10) * 10;

    }
}




