package com.Auroral.blog.dao;

import com.Auroral.blog.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Auroral
 * @since 2020-05-18
 */
@Repository
public interface MessageDao extends BaseMapper<Message> {

}
