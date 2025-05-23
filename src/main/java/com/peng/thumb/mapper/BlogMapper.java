package com.peng.thumb.mapper;

import com.peng.thumb.model.entity.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

/**
 * @author pjw
 * @description 针对表【blog】的数据库操作Mapper
 * @createDate 2025-04-27 01:38:49
 * @Entity com.peng.thumb.model.entity.Blog
 */
public interface BlogMapper extends BaseMapper<Blog> {

    void BatchUpdateThumbCount(@Param("countMap") Map<Long, Long> countMap);
}




