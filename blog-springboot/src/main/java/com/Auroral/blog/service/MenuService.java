package com.Auroral.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.Auroral.blog.dto.MenuDTO;
import com.Auroral.blog.dto.labelOptionDTO;
import com.Auroral.blog.dto.UserMenuDTO;
import com.Auroral.blog.entity.Menu;
import com.Auroral.blog.vo.ConditionVO;

import java.util.List;

/**
 * @author: Auroral
 * @date: 2021-01-23
 **/
public interface MenuService extends IService<Menu> {

    /**
     * 查看菜单列表
     * @param conditionVO 条件
     * @return 菜单列表
     */
    List<MenuDTO> listMenus(ConditionVO conditionVO);

    /**
     * 查看角色菜单选项
     * @return 角色菜单选项
     */
    List<labelOptionDTO> listMenuOptions();

    /**
     * 查看用户菜单
     * @return 菜单列表
     */
    List<UserMenuDTO> listUserMenus();

}
