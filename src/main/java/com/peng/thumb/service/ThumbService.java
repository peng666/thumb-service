package com.peng.thumb.service;

import com.peng.thumb.model.dto.thumb.DoThumbRequest;
import com.peng.thumb.model.entity.Thumb;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author pjw
 * @description 针对表【thumb】的数据库操作Service
 * @createDate 2025-04-27 01:38:39
 */
public interface ThumbService extends IService<Thumb> {

    /**
     * 点赞
     *
     * @param doThumbRequest
     * @param request
     * @return
     */
    Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);

    /**
     * 取消点赞
     *
     * @param doThumbRequest
     * @param request
     * @return
     */
    Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);
}
