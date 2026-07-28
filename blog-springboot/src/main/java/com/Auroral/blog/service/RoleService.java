package com.Auroral.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.Auroral.blog.dto.PageDTO;
import com.Auroral.blog.dto.RoleDTO;
import com.Auroral.blog.dto.UserRoleDTO;
import com.Auroral.blog.entity.Role;
import com.Auroral.blog.vo.ConditionVO;
import com.Auroral.blog.vo.RoleVO;

import java.util.List;

/**
 * @author: Auroral
 * @date: 2020-12-27
 **/
public interface RoleService extends IService<Role> {

    /**
     * 获取用户角色选项
     *
     * @return 角色
     */
    List<UserRoleDTO> listUserRoles();

    /**
     * 查询角色列表
     *
     * @param conditionVO 条件
     * @return 角色列表
     */
    PageDTO<RoleDTO> listRoles(ConditionVO conditionVO);

    /**
     * 保存或更新角色
     *
     * @param roleVO 角色
     */
    void saveOrUpdateRole(RoleVO roleVO);

    /**
     * 删除角色
     * @param roleIdList 角色id列表
     */
    void deleteRoles(List<Integer> roleIdList);

}
