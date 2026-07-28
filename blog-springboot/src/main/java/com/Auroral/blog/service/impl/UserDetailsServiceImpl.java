package com.Auroral.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.Auroral.blog.dao.*;
import com.Auroral.blog.entity.UserAuth;
import com.Auroral.blog.entity.UserInfo;
import com.Auroral.blog.exception.ServeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.Auroral.blog.constant.RedisPrefixConst.*;
import static com.Auroral.blog.utils.UserUtil.convertLoginUser;


/**
 * @author Auroral
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UserAuthDao userAuthDao;
    @Autowired
    private UserInfoDao userInfoDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private RedisTemplate redisTemplate;
    @Resource
    private HttpServletRequest request;

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new ServeException("用户名不能为空！");
        }

        // 查询账号是否存在
        UserAuth user = userAuthDao.selectOne(new LambdaQueryWrapper<UserAuth>()
                .select(UserAuth::getId, UserAuth::getUserInfoId, UserAuth::getUsername, UserAuth::getPassword, UserAuth::getLoginType)
                .eq(UserAuth::getUsername, username));
        if (Objects.isNull(user)) {
            throw new ServeException("用户名不存在!");
        }

        // 查询账号信息
        UserInfo userInfo = userInfoDao.selectOne(new LambdaQueryWrapper<UserInfo>()
                .select(UserInfo::getId, UserInfo::getEmail, UserInfo::getNickname, UserInfo::getAvatar, UserInfo::getIntro, UserInfo::getWebSite, UserInfo::getIsDisable)
                .eq(UserInfo::getId, user.getUserInfoId()));
        if (Objects.isNull(userInfo)) {
            throw new ServeException("用户信息不存在!");
        }

        // 查询账号角色
        List<String> roleList = roleDao.listRolesByUserInfoId(userInfo.getId());
        if (roleList == null) {
            roleList = new ArrayList<>(); // 防止空指针异常
        }

        // 查询账号点赞信息 - 安全获取Redis数据
        Set<Integer> articleLikeSet = null;
        Set<Integer> commentLikeSet = null;

        try {
            articleLikeSet = (Set<Integer>) redisTemplate.boundHashOps(ARTICLE_USER_LIKE).get(userInfo.getId().toString());
            commentLikeSet = (Set<Integer>) redisTemplate.boundHashOps(COMMENT_USER_LIKE).get(userInfo.getId().toString());
        } catch (Exception e) {
            // 记录日志但不阻止登录流程
            System.err.println("获取Redis点赞数据异常: " + e.getMessage());
        }

        // 为空时初始化为空集合
        if (articleLikeSet == null) {
            articleLikeSet = new HashSet<>();
        }
        if (commentLikeSet == null) {
            commentLikeSet = new HashSet<>();
        }

        // 封装登录信息
        return convertLoginUser(user, userInfo, roleList, articleLikeSet, commentLikeSet, request);
    }

}
