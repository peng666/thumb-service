package com.peng.thumb.service;

import com.peng.thumb.model.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;
import com.peng.thumb.model.vo.BolgVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * @author pjw
 * @description 针对表【blog】的数据库操作Service
 * @createDate 2025-04-27 01:38:49
 */
public interface BlogService extends IService<Blog> {
    BolgVO getBlogVOById(Long blogId, HttpServletRequest request);

    List<BolgVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request);

}
