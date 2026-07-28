package com.Auroral.blog.controller;

import com.Auroral.blog.annotation.OptLog;
import com.Auroral.blog.constant.StatusConst;
import com.Auroral.blog.dto.NoticeDTO;
import com.Auroral.blog.service.NoticeService;
import com.Auroral.blog.vo.NoticeVO;
import com.Auroral.blog.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static com.Auroral.blog.constant.OptTypeConst.*;

/**
 * 公告控制器
 */
@Api(tags = "公告模块")
@RestController
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * 查看后台公告列表
     *
     * @return {@link Result<List<NoticeDTO>>} 公告列表
     */
    @ApiOperation(value = "查看后台公告列表")
    @GetMapping("/admin/notices")
    public Result<List<NoticeDTO>> listNotices() {
        return new Result<>(true, StatusConst.OK, "查询成功", noticeService.listNotices());
    }

    /**
     * 添加公告
     *
     * @param noticeVO 公告信息
     * @return {@link Result<>}
     */
    @OptLog(optType = SAVE)
    @ApiOperation(value = "添加公告")
    @PostMapping("/admin/notices")
    public Result<?> saveNotice(@Valid @RequestBody NoticeVO noticeVO) {
        noticeService.saveOrUpdateNotice(noticeVO);
        return new Result<>(true, StatusConst.OK, "添加成功");
    }

    /**
     * 修改公告
     *
     * @param noticeVO 公告信息
     * @return {@link Result<>}
     */
    @OptLog(optType = UPDATE)
    @ApiOperation(value = "修改公告")
    @PutMapping("/admin/notices/{noticeId}")
    public Result<?> updateNotice(@PathVariable("noticeId") Integer noticeId,
                                 @Valid @RequestBody NoticeVO noticeVO) {
        noticeVO.setId(noticeId);
        noticeService.saveOrUpdateNotice(noticeVO);
        return new Result<>(true, StatusConst.OK, "修改成功");
    }

    /**
     * 删除公告
     *
     * @param noticeId 公告id
     * @return {@link Result<>}
     */
    @OptLog(optType = REMOVE)
    @ApiOperation(value = "删除公告")
    @DeleteMapping("/admin/notices/{noticeId}")
    public Result<?> deleteNotice(@PathVariable("noticeId") Integer noticeId) {
        noticeService.deleteNotice(noticeId);
        return new Result<>(true, StatusConst.OK, "删除成功");
    }
}