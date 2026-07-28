package com.Auroral.blog.service.impl;

import com.Auroral.blog.dao.FavoriteCategoryMapper;
import com.Auroral.blog.dao.FavoriteMapper;
import com.Auroral.blog.dao.FavoriteTagMapper;
import com.Auroral.blog.dto.FavoriteCategoryDTO;
import com.Auroral.blog.dto.FavoriteDTO;
import com.Auroral.blog.entity.Favorite;
import com.Auroral.blog.entity.FavoriteCategory;
import com.Auroral.blog.entity.FavoriteTag;
import com.Auroral.blog.service.FavoriteService;
import com.Auroral.blog.vo.FavoriteCategoryVO;
import com.Auroral.blog.vo.FavoriteVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 收藏网站服务实现
 *
 * @author Auroral
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteCategoryMapper favoriteCategoryMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private FavoriteTagMapper favoriteTagMapper;

    @Override
    public List<FavoriteCategoryDTO> listFavoriteCategories() {
        // 查询收藏网站分类列表
        List<FavoriteCategory> categoryList = favoriteCategoryMapper.selectList(
                new LambdaQueryWrapper<FavoriteCategory>()
                        .orderByAsc(FavoriteCategory::getSort)
        );
        
        // 转换为DTO
        return categoryList.stream()
                .map(category -> {
                    FavoriteCategoryDTO categoryDTO = new FavoriteCategoryDTO();
                    BeanUtils.copyProperties(category, categoryDTO);
                    return categoryDTO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<FavoriteDTO> listFavorites(Integer categoryId) {
        return favoriteMapper.listFavorites(categoryId);
    }

    @Override
    public FavoriteDTO getFavoriteById(Integer id) {
        return favoriteMapper.getFavoriteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateFavoriteCategory(FavoriteCategoryVO favoriteCategoryVO) {
        // 创建收藏网站分类
        FavoriteCategory favoriteCategory = new FavoriteCategory();
        BeanUtils.copyProperties(favoriteCategoryVO, favoriteCategory);
        
        this.favoriteCategoryMapper.insert(favoriteCategory);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateFavorite(FavoriteVO favoriteVO) {
        // 保存收藏网站信息
        Favorite favorite = new Favorite();
        BeanUtils.copyProperties(favoriteVO, favorite);
        
        if (Objects.isNull(favoriteVO.getId())) {
            // 新增
            favoriteMapper.insert(favorite);
        } else {
            // 修改
            favoriteMapper.updateById(favorite);
            
            // 删除原有标签
            favoriteTagMapper.delete(new LambdaQueryWrapper<FavoriteTag>()
                    .eq(FavoriteTag::getFavoriteId, favorite.getId()));
        }
        
        // 保存标签
        if (CollectionUtils.isNotEmpty(favoriteVO.getTags())) {
            List<FavoriteTag> tagList = favoriteVO.getTags().stream()
                    .map(tag -> FavoriteTag.builder()
                            .favoriteId(favorite.getId())
                            .tagName(tag)
                            .build())
                    .collect(Collectors.toList());
            for (FavoriteTag tag : tagList) {
                favoriteTagMapper.insert(tag);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteFavoriteCategory(Integer categoryId) {
        // 查询分类下是否有收藏网站
        List<Favorite> favoriteList = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getCategoryId, categoryId)
        );
        
        if (CollectionUtils.isNotEmpty(favoriteList)) {
            // 有收藏网站，不能删除
            throw new RuntimeException("该分类下有收藏网站，无法删除");
        }
        
        // 删除分类
        favoriteCategoryMapper.deleteById(categoryId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteFavorite(Integer favoriteId) {
        // 删除收藏网站
        favoriteMapper.deleteById(favoriteId);
        
        // 删除相关标签
        favoriteTagMapper.delete(new LambdaQueryWrapper<FavoriteTag>()
                .eq(FavoriteTag::getFavoriteId, favoriteId));
    }
}