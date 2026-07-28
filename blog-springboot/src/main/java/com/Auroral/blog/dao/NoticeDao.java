package com.Auroral.blog.dao;

import com.Auroral.blog.entity.Notice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

/**
 * 公告Mapper
 */
@Repository
public interface NoticeDao extends BaseMapper<Notice> {
}