package com.peng.thumb.service;

import com.peng.thumb.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author pjw
 * @description 针对表【user】的数据库操作Service
 * @createDate 2025-04-27 01:38:03
 */
public interface UserService extends IService<User> {

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);
}
