package com.Auroral.blog.controller;


import com.Auroral.blog.constant.StatusConst;
import com.Auroral.blog.dto.PageDTO;
import com.Auroral.blog.dto.UserBackDTO;
import com.Auroral.blog.dto.UserInfoDTO;
import com.Auroral.blog.service.UserAuthService;
import com.Auroral.blog.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/***
 * @author Auroral
 * @since 2020-05-18
 */
@Api(tags = "用户账号模块")
@RestController
public class UserAuthController {
    @Autowired
    private UserAuthService userAuthService;

    @ApiOperation(value = "发送邮箱验证码")
    @ApiImplicitParam(name = "username", value = "用户名", required = true, dataType = "String")
    @GetMapping("/users/code")
    public Result sendCode(String username) {
        userAuthService.sendCode(username);
        return new Result<>(true, StatusConst.OK, "发送成功！");
    }

    /**
     * 发送手机验证码
     */
    @ApiOperation(value = "发送手机验证码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", value = "手机号", required = true, dataType = "String")
    })
    @PostMapping("/users/sms/code")
    public Result<?> sendSmsCode(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        userAuthService.sendSmsCode(username);
        return new Result<>(true, StatusConst.OK, "发送成功！");
    }

    @ApiOperation(value = "查看后台用户列表")
    @GetMapping("/admin/users")
    public Result<PageDTO<UserBackDTO>> listUsers(ConditionVO condition) {
        return new Result<>(true, StatusConst.OK, "查询成功！", userAuthService.listUserBackDTO(condition));
    }

    @ApiOperation(value = "用户注册")
    @PostMapping("/users")
    public Result saveUser(@Valid @RequestBody UserVO user) {
        userAuthService.saveUser(user);
        return new Result<>(true, StatusConst.OK, "注册成功！");
    }

    @ApiOperation(value = "修改密码")
    @PutMapping("/users/password")
    public Result updatePassword(@Valid @RequestBody UserVO user) {
        userAuthService.updatePassword(user);
        return new Result<>(true, StatusConst.OK, "修改成功！");
    }

//
    @ApiOperation(value = "修改管理员密码")
    @PutMapping("/admin/users/password")
    public Result updateAdminPassword(@Valid @RequestBody PasswordVO passwordVO) {
        userAuthService.updateAdminPassword(passwordVO);
        return new Result<>(true, StatusConst.OK, "修改成功！");
    }

    @ApiOperation(value = "微博登录")
    @ApiImplicitParam(name = "code", value = "code", required = true, dataType = "String")
    @GetMapping("/users/oauth/weibo") // 从POST改为GET
    public Result<UserInfoDTO> weiBoLogin(String code) {
        try {
            UserInfoDTO userInfo = userAuthService.weiBoLogin(code);
            return new Result<>(true, StatusConst.OK, "登录成功！", userInfo);
        } catch (Exception e) {
            return new Result<>(false, StatusConst.ERROR, "登录失败：" + e.getMessage(), null);
        }
    }
    @ApiOperation(value = "GitHub登录")
    @ApiImplicitParam(name = "code", value = "code", required = true, dataType = "String")
    @GetMapping("/users/oauth/github")
    public Result<UserInfoDTO> githubLogin(String code) {
        try {
            UserInfoDTO userInfo = userAuthService.githubLogin(code);
            return new Result<>(true, StatusConst.OK, "登录成功！", userInfo);
        } catch (Exception e) {
            return new Result<>(false, StatusConst.ERROR, "登录失败：" + e.getMessage(), null);
        }
    }
    @ApiOperation(value = "qq登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "openId", value = "openId", required = true, dataType = "String"),
            @ApiImplicitParam(name = "accessToken", value = "accessToken", required = true, dataType = "String")
    })
    @PostMapping("/users/oauth/qq")
    public Result<UserInfoDTO> qqLogin(String openId, String accessToken) {
        return new Result<>(true, StatusConst.OK, "登录成功！", userAuthService.qqLogin(openId, accessToken));
    }

    @ApiOperation(value = "QQ登录回调")
    @ApiImplicitParam(name = "code", value = "QQ授权码", required = true, dataType = "String")
    @GetMapping("/users/oauth/qq/callback")
    public Result<UserInfoDTO> qqLoginCallback(String code) {
        try {
            UserInfoDTO userInfo = userAuthService.qqLoginByCode(code);
            return new Result<>(true, StatusConst.OK, "登录成功！", userInfo);
        } catch (Exception e) {
            e.printStackTrace(); // 记录详细错误
            return new Result<>(false, StatusConst.ERROR, "登录失败：" + e.getMessage(), null);
        }
    }

}

