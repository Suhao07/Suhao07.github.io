package com.Auroral.blog.dao;

import com.Auroral.blog.dto.FavoriteDTO;
import com.Auroral.blog.entity.Favorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 收藏网站Mapper
 *
 * @author Auroral
 */
@Repository
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 查询收藏网站列表
     *
     * @param categoryId 分类ID
     * @return 收藏网站列表
     */
    List<FavoriteDTO> listFavorites(@Param("categoryId") Integer categoryId);

    /**
     * 根据ID查询收藏网站详情
     *
     * @param id 收藏网站ID
     * @return 收藏网站详情
     */
    FavoriteDTO getFavoriteById(@Param("id") Integer id);
}