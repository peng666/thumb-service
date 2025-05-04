package com.peng.thumb.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.peng.thumb.constant.ThumbConstant;
import com.peng.thumb.model.entity.Blog;
import com.peng.thumb.model.entity.Thumb;
import com.peng.thumb.model.entity.User;
import com.peng.thumb.model.vo.BolgVO;
import com.peng.thumb.service.BlogService;
import com.peng.thumb.mapper.BlogMapper;
import com.peng.thumb.service.ThumbService;
import com.peng.thumb.service.UserService;
import com.peng.thumb.util.RedisKeyUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author pjw
 * @description 针对表【blog】的数据库操作Service实现
 * @createDate 2025-04-27 01:38:49
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
        implements BlogService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private ThumbService thumbService;

    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 获取博客的VO
     *
     * @param blogId  博客ID
     * @param request 请求对象，获取当前登录的用户
     * @return
     */
    @Override
    public BolgVO getBlogVOById(Long blogId, HttpServletRequest request) {
        Blog blog = this.getById(blogId);
        User loginUser = userService.getLoginUser(request);
        return this.getBlogVO(blog, loginUser);
    }

    private BolgVO getBlogVO(Blog blog, User loginUser) {
        BolgVO bolgVO = new BolgVO();
        BeanUtils.copyProperties(blog, bolgVO);
        if (ObjectUtils.isEmpty(loginUser)) {
            return bolgVO;
        }


        Boolean exist = thumbService.hasThumb(blog.getId(), loginUser.getId());
        bolgVO.setHasThumb(exist);
        return bolgVO;
    }

    @Override
    public List<BolgVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Map<Long, Boolean> blogIdHasThumbMap = new HashMap<>();
        if (ObjectUtils.isNotEmpty(loginUser)) {
            List<Object> blogIdList = blogList.stream().map(blog -> blog.getId().toString()).collect(Collectors.toList());
            List<Object> thumbIdList = redisTemplate.opsForHash().multiGet(RedisKeyUtil.getUserThumbKey(loginUser.getId()), blogIdList);
            for (int i = 0; i < thumbIdList.size(); i++) {
                if (ObjectUtils.isEmpty(thumbIdList.get(i))) {
                    continue;
                }
                blogIdHasThumbMap.put(Long.valueOf(blogIdList.get(i).toString()), true);
            }
        }

        return blogList.stream().map(blog -> {
            BolgVO bolgVO = BeanUtil.copyProperties(blog, BolgVO.class);
            bolgVO.setHasThumb(blogIdHasThumbMap.get(blog.getId()));
            return bolgVO;
        }).collect(Collectors.toList());
    }
}




