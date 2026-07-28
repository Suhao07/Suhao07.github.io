package com.Auroral.blog.service;

import com.Auroral.blog.dto.FriendLinkBackDTO;
import com.Auroral.blog.dto.FriendLinkDTO;
import com.Auroral.blog.dto.PageDTO;
import com.Auroral.blog.entity.FriendLink;
import com.baomidou.mybatisplus.extension.service.IService;
import com.Auroral.blog.vo.ConditionVO;
import com.Auroral.blog.vo.FriendLinkVO;

import java.util.List;

/**
 * @author Auroral
 * @since 2020-05-18
 */
public interface FriendLinkService extends IService<FriendLink> {

    /**
     * 查看友链列表
     *
     * @return 友链列表
     */
    List<FriendLinkDTO> listFriendLinks();

    /**
     * 查看后台友链列表
     *
     * @param condition 条件
     * @return 友链列表
     */
    PageDTO<FriendLinkBackDTO> listFriendLinkDTO(ConditionVO condition);

    /**
     * 保存或更新友链
     * @param friendLinkVO 友链
     */
    void saveOrUpdateFriendLink(FriendLinkVO friendLinkVO);

}
