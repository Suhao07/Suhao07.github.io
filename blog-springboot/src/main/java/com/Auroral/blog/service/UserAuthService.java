package com.Auroral.blog.service;

import com.Auroral.blog.dto.PageDTO;
import com.Auroral.blog.dto.UserBackDTO;
import com.Auroral.blog.dto.UserInfoDTO;
import com.Auroral.blog.entity.UserAuth;
import com.baomidou.mybatisplus.extension.service.IService;
import com.Auroral.blog.vo.ConditionVO;
import com.Auroral.blog.vo.PasswordVO;
import com.Auroral.blog.vo.UserVO;


/**
 * @author Auroral
 * @since 2020-05-18
 */
public interface UserAuthService extends IService<UserAuth> {

    /**
     * 发送邮箱验证码
     *
     * @param username 邮箱号
     */
    void sendCode(String username);

    /**
     * 用户注册
     *
     * @param user 用户对象
     */
    void saveUser(UserVO user);

    /**
     * qq登录
     *
     * @param openId      qq openId
     * @param accessToken qq token
     * @return 用户登录信息
     */
    UserInfoDTO qqLogin(String openId, String accessToken);

    /**
     * 微博登录
     *
     * @param code 微博code
     * @return 用户登录信息
     */
    UserInfoDTO weiBoLogin(String code);

    /**
     * 修改密码
     *
     * @param user 用户对象
     */
    void updatePassword(UserVO user);

    /**
     * 修改管理员密码
     *
     * @param passwordVO 密码对象
     */
    void updateAdminPassword(PasswordVO passwordVO);

    /**
     * 查询后台用户列表
     *
     * @param condition 条件
     * @return 用户列表
     */
    PageDTO<UserBackDTO> listUserBackDTO(ConditionVO condition);
    /**
     * 发送短信验证码
     *
     * @param username 手机号
     */
    void sendSmsCode(String username);
    /**
     * GitHub登录
     *
     * @param code GitHub code
     * @return 用户登录信息
     */
    UserInfoDTO githubLogin(String code);

    /**
     * 使用code方式QQ登录
     *
     * @param code QQ授权码
     * @return 用户登录信息
     */
    UserInfoDTO qqLoginByCode(String code);
}
