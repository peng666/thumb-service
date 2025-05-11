package com.peng.thumb.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.peng.thumb.constant.ThumbConstant;
import com.peng.thumb.util.RedisKeyUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class SyncThumb2DBCompensatoryJob {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private SyncThumb2DBJob syncThumb2DBJob;

//    @Scheduled(cron = "0 0 2 * * *")
    public void run() {
        log.info("开始补偿任务");
        Set<String> thumbKeys = redisTemplate.keys(RedisKeyUtil.getTempThumbKey("") + "*"); // 查出所有的key
        Set<String> needHandleDataSet = new HashSet<>(); // 存放需要补偿的key

        thumbKeys.stream().filter(ObjUtil::isNotNull).forEach(thumbKey ->
                needHandleDataSet.add(thumbKey.replace(ThumbConstant.USER_THUMB_KEY_PREFIX.formatted(""), "")));
        if (CollUtil.isEmpty(needHandleDataSet)) {
            log.info("没有需要补偿的临时数据");
            return;
        }
        for (String date : needHandleDataSet) {
            syncThumb2DBJob.syncThumb2DBBYDate(date);
        }
        log.info("临时数据补偿完成");
    }
}
