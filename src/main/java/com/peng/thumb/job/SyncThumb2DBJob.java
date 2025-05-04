package com.peng.thumb.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peng.thumb.mapper.BlogMapper;
import com.peng.thumb.model.entity.Thumb;
import com.peng.thumb.model.enums.ThumbTypeEnum;
import com.peng.thumb.service.ThumbService;
import com.peng.thumb.util.RedisKeyUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SyncThumb2DBJob {
    @Resource
    private ThumbService thumbService;

    @Resource
    private BlogMapper blogMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 10000)
    @Transactional(rollbackFor = Exception.class)
    public void run() {
        log.info("开始执行同步任务");
        DateTime nowDate = DateUtil.date();
        int second = (DateUtil.second(nowDate) / 10 - 1) * 10;
        if (second == -10) {
            second = 50;
            nowDate = DateUtil.offsetMinute(nowDate, -1);
        }
        String date = DateUtil.format(nowDate, "HH:mm:") + second; // 获取redis的key的时间

        syncThumb2DBBYDate(date);
        log.info("临时同步数据完成");
    }

    public void syncThumb2DBBYDate(String date) {
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(date);  // 生成redis的key
        Map<Object, Object> allTempThumbMap = redisTemplate.opsForHash().entries(tempThumbKey); // redis查出来这段时间（本次要同步的）数据
        if (CollUtil.isEmpty(allTempThumbMap)) {
            return;
        }

        HashMap<Long, Long> blogThumbCountMap = new HashMap<>();
        ArrayList<Thumb> thumbList = new ArrayList<>();  // 新增点赞。存储每一条点赞记录
        LambdaQueryWrapper<Thumb> wrapper = new LambdaQueryWrapper<>(); // 取消点赞的条件查询
        boolean needRemove = false;


        for (Object userIdBlogIdObj : allTempThumbMap.keySet()) { // 遍历每一条点赞数据
            String userIdBlogId = (String) userIdBlogIdObj;
            String[] userIdAndBlogId = userIdBlogId.split(StrPool.COLON);
            Long userId = Long.valueOf(userIdAndBlogId[0]);
            Long blogId = Long.valueOf(userIdAndBlogId[1]);
            Integer thumbType = Integer.valueOf(allTempThumbMap.get(userIdBlogId).toString()); // 获取该条数据的值0,1，-1？
            if (thumbType == ThumbTypeEnum.INCR.getValue()) { // 点赞
                Thumb thumb = new Thumb();
                thumb.setUserId(userId);
                thumb.setBlogId(blogId);
                thumbList.add(thumb); // 放进点赞表里
            } else if (thumbType == ThumbTypeEnum.DECR.getValue()) { // 取消点赞
                needRemove = true;
                wrapper.or().eq(Thumb::getUserId, userId).eq(Thumb::getBlogId, blogId);
            } else {
                if (thumbType != ThumbTypeEnum.NON.getValue()) { // 未知操作
                    log.warn("数据异常： {}", userId + "," + blogId, "," + thumbType);
                }
                continue;
            }
            // 计算博客在blog表的点赞数，放在map中
            blogThumbCountMap.put(blogId, blogThumbCountMap.getOrDefault(blogId, 0L) + thumbType);
        }

        // 批量插入
        thumbService.saveBatch(thumbList);
        // 批量删除
        if (needRemove) {
            thumbService.remove(wrapper);
        }
        // 批量更新博客点赞量
        if (ObjectUtils.isNotEmpty(blogThumbCountMap)) {
            log.info("批量更新博客点赞量：{}", blogThumbCountMap);
            blogMapper.BatchUpdateThumbCount(blogThumbCountMap);
        }
        // 异步删除
        Thread.startVirtualThread(() -> {
            log.info("删除redis临时数据");
            redisTemplate.delete(tempThumbKey);
        });
    }
}
