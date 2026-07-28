package com.Auroral.blog.controller;

import com.Auroral.blog.constant.StatusConst;
import com.Auroral.blog.dto.OperationLogDTO;
import com.Auroral.blog.dto.PageDTO;
import com.Auroral.blog.service.OperationLogService;
import com.Auroral.blog.vo.ConditionVO;
import com.Auroral.blog.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author: Auroral
 * @date: 2021-01-31
 **/
@Api(tags = "日志模块")
@RestController
public class LogController {
    @Autowired
    private OperationLogService operationLogService;

    @ApiOperation(value = "查看操作日志")
    @GetMapping("/admin/operation/logs")
    public Result<PageDTO<OperationLogDTO>> listOperationLogs(ConditionVO conditionVO) {
        return new Result<>(true, StatusConst.OK, "查询成功", operationLogService.listOperationLogs(conditionVO));
    }

    @ApiOperation(value = "删除操作日志")
    @DeleteMapping("/admin/operation/logs")
    public Result deleteOperationLogs(@RequestBody List<Integer> logIdList) {
        operationLogService.removeByIds(logIdList);
        return new Result<>(true, StatusConst.OK, "删除成功");
    }

}
