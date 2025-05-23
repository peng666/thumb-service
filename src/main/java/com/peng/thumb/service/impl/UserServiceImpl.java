package com.peng.thumb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.peng.thumb.constant.UserConstant;
import com.peng.thumb.model.entity.User;
import com.peng.thumb.service.UserService;
import com.peng.thumb.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
* @author pjw
* @description 针对表【user】的数据库操作Service实现
* @createDate 2025-04-27 01:38:03
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(UserConstant.LOGIN_USER);
    }
}




