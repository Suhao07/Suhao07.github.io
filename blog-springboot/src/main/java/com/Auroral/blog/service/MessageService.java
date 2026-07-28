package com.Auroral.blog.service;

import com.Auroral.blog.dto.MessageBackDTO;
import com.Auroral.blog.dto.PageDTO;
import com.Auroral.blog.vo.ConditionVO;
import com.Auroral.blog.vo.MessageVO;
import com.Auroral.blog.dto.MessageDTO;
import com.Auroral.blog.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author Auroral
 * @since 2020-05-18
 */
public interface MessageService extends IService<Message> {

    /**
     * 添加留言弹幕
     *
     * @param messageVO 留言对象
     */
    void saveMessage(MessageVO messageVO);

    /**
     * 查看留言弹幕
     *
     * @return 留言列表
     */
    List<MessageDTO> listMessages();

    /**
     * 查看后台留言
     *
     * @param condition 条件
     * @return 留言列表
     */
    PageDTO<MessageBackDTO> listMessageBackDTO(ConditionVO condition);

}
