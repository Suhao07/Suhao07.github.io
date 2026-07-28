package com.Auroral.blog.service;

import com.Auroral.blog.dto.NoticeDTO;
import com.Auroral.blog.vo.NoticeVO;

import java.util.List;

/**
 * 公告服务接口
 */
public interface NoticeService {

    /**
     * 获取所有公告列表
     * @return 公告列表
     */
    List<NoticeDTO> listNotices();

    /**
     * 添加或修改公告
     * @param noticeVO 公告信息
     */
    void saveOrUpdateNotice(NoticeVO noticeVO);

    /**
     * 根据id删除公告
     * @param noticeId 公告id
     */
    void deleteNotice(Integer noticeId);

    /**
     * 获取最新的几条公告
     * @param limit 获取数量
     * @return 公告列表
     */
    List<NoticeDTO> listLatestNotices(int limit);
}