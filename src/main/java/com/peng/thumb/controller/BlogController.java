package com.peng.thumb.controller;

import com.peng.thumb.common.BaseResponse;
import com.peng.thumb.common.ResultUtils;
import com.peng.thumb.model.entity.Blog;
import com.peng.thumb.model.vo.BolgVO;
import com.peng.thumb.service.BlogService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("blog")
public class BlogController {

    @Resource
    BlogService blogService;

    @GetMapping("/get")
    public BaseResponse<BolgVO> get(Long blogId, HttpServletRequest request) {
        BolgVO blogVOById = blogService.getBlogVOById(blogId, request);
        return ResultUtils.success(blogVOById);
    }

    @GetMapping("/list")
    public BaseResponse<List<BolgVO>> list(HttpServletRequest request) {
        List<Blog> blogList = blogService.list(); // 不是项目重点，不做各种条件查询
        List<BolgVO> blogVOList = blogService.getBlogVOList(blogList, request);
        return ResultUtils.success(blogVOList);
    }

}
