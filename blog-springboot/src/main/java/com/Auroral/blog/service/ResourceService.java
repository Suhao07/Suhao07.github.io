package com.Auroral.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.Auroral.blog.dto.ResourceDTO;
import com.Auroral.blog.dto.labelOptionDTO;
import com.Auroral.blog.entity.Resource;
import com.Auroral.blog.vo.ResourceVO;

import java.util.List;


/**
 * @author: Auroral
 * @date: 2020-12-27
 **/
public interface ResourceService extends IService<Resource> {

    /**
     * 导入swagger权限
     */
    void importSwagger();

    /**
     * 添加或修改资源
     * @param resourceVO 资源对象
     */
    void saveOrUpdateResource(ResourceVO resourceVO);

    /***
     * 删除资源
     * @param resourceIdList 资源id列表
     */
    void deleteResources(List<Integer> resourceIdList);

    /**
     * 查看资源列表
     *
     * @return 资源列表
     */
    List<ResourceDTO> listResources();

    /**
     * 查看资源选项
     * @return 资源选项
     */
    List<labelOptionDTO> listResourceOption();

}
