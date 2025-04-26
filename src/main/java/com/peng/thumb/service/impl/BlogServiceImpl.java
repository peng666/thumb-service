package com.peng.thumb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.peng.thumb.model.entity.Blog;
import com.peng.thumb.service.BlogService;
import com.peng.thumb.mapper.BlogMapper;
import org.springframework.stereotype.Service;

/**
* @author pjw
* @description 针对表【blog】的数据库操作Service实现
* @createDate 2025-04-27 01:38:49
*/
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
    implements BlogService{

}




