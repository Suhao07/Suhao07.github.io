package com.Auroral.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.Auroral.blog.entity.OperationLog;
import org.springframework.stereotype.Repository;

/**
 * @author: Auroral
 * @date: 2021-01-31
 **/
@Repository
public interface OperationLogDao extends BaseMapper<OperationLog> {
}
