package com.Auroral.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.Auroral.blog.dto.OperationLogDTO;
import com.Auroral.blog.dto.PageDTO;
import com.Auroral.blog.entity.OperationLog;
import com.Auroral.blog.vo.ConditionVO;

/**
 * @author: Auroral
 * @date: 2021-01-31
 **/
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 查询日志列表
     *
     * @param conditionVO 条件
     * @return 日志列表
     */
    PageDTO<OperationLogDTO> listOperationLogs(ConditionVO conditionVO);

}
