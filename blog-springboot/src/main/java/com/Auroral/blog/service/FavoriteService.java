package com.Auroral.blog.service;

import com.Auroral.blog.dto.FavoriteCategoryDTO;
import com.Auroral.blog.dto.FavoriteDTO;
import com.Auroral.blog.vo.FavoriteCategoryVO;
import com.Auroral.blog.vo.FavoriteVO;

import java.util.List;

/**
 * 收藏网站服务
 *
 * @author Auroral
 */
public interface FavoriteService {

    /**
     * 查询收藏网站分类列表
     *
     * @return 收藏网站分类列表
     */
    List<FavoriteCategoryDTO> listFavoriteCategories();

    /**
     * 查询收藏网站列表
     *
     * @param categoryId 分类ID
     * @return 收藏网站列表
     */
    List<FavoriteDTO> listFavorites(Integer categoryId);

    /**
     * 根据ID查询收藏网站详情
     *
     * @param id 收藏网站ID
     * @return 收藏网站详情
     */
    FavoriteDTO getFavoriteById(Integer id);

    /**
     * 添加或修改收藏网站分类
     *
     * @param favoriteCategoryVO 收藏网站分类信息
     */
    void saveOrUpdateFavoriteCategory(FavoriteCategoryVO favoriteCategoryVO);

    /**
     * 添加或修改收藏网站
     *
     * @param favoriteVO 收藏网站信息
     */
    void saveOrUpdateFavorite(FavoriteVO favoriteVO);

    /**
     * 删除收藏网站分类
     *
     * @param categoryId 分类ID
     */
    void deleteFavoriteCategory(Integer categoryId);

    /**
     * 删除收藏网站
     *
     * @param favoriteId 收藏网站ID
     */
    void deleteFavorite(Integer favoriteId);
}