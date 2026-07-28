package com.Auroral.blog.dao;

import com.Auroral.blog.dto.UserBackDTO;
import com.Auroral.blog.entity.UserAuth;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.Auroral.blog.vo.ConditionVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 *
 * @author Auroral
 * @since 2020-05-18
 */
@Repository
public interface UserAuthDao extends BaseMapper<UserAuth> {

    /**
     * 查询后台用户列表
     * @param condition 条件
     * @return 用户集合
     */
    List<UserBackDTO> listUsers(@Param("condition") ConditionVO condition);

    /**
     * 查询后台用户数量
     * @param condition 条件
     * @return 用户数量
     */
    Integer countUser(@Param("condition") ConditionVO condition);

}
