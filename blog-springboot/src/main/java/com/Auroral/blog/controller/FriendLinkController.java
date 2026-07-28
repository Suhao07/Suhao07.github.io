package com.Auroral.blog.controller;


import com.Auroral.blog.annotation.OptLog;
import com.Auroral.blog.constant.StatusConst;
import com.Auroral.blog.dto.FriendLinkBackDTO;
import com.Auroral.blog.dto.FriendLinkDTO;
import com.Auroral.blog.dto.PageDTO;
import com.Auroral.blog.service.FriendLinkService;
import com.Auroral.blog.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static com.Auroral.blog.constant.OptTypeConst.SAVE_OR_UPDATE;
import static com.Auroral.blog.constant.OptTypeConst.REMOVE;

/**
 * @author Auroral
 * @since 2020-05-18
 */
@Api(tags = "友链模块")
@RestController
public class FriendLinkController {
    @Autowired
    private FriendLinkService friendLinkService;

    @ApiOperation(value = "查看友链列表")
    @GetMapping("/links")
    public Result<List<FriendLinkDTO>> listFriendLinks() {
        return new Result<>(true, StatusConst.OK, "查询成功", friendLinkService.listFriendLinks());
    }

    @ApiOperation(value = "查看后台友链列表")
    @GetMapping("/admin/links")
    public Result<PageDTO<FriendLinkBackDTO>> listFriendLinkDTO(ConditionVO condition) {
        return new Result<>(true, StatusConst.OK, "查询成功", friendLinkService.listFriendLinkDTO(condition));
    }

    @OptLog(optType = SAVE_OR_UPDATE)
    @ApiOperation(value = "保存或修改友链")
    @PostMapping("/admin/links")
    public Result saveOrUpdateFriendLink(@Valid @RequestBody FriendLinkVO friendLinkVO) {
        friendLinkService.saveOrUpdateFriendLink(friendLinkVO);
        return new Result<>(true, StatusConst.OK, "操作成功");
    }

    @OptLog(optType = REMOVE)
    @ApiOperation(value = "删除友链")
    @DeleteMapping("/admin/links")
    public Result deleteFriendLink(@RequestBody List<Integer> linkIdList) {
        friendLinkService.removeByIds(linkIdList);
        return new Result<>(true, StatusConst.OK, "删除成功");
    }

}

