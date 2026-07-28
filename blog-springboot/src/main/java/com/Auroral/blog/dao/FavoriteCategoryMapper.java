package com.Auroral.blog.dao;

import com.Auroral.blog.entity.FavoriteCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

/**
 * 收藏网站分类Mapper
 *
 * @author Auroral
 */
@Repository
public interface FavoriteCategoryMapper extends BaseMapper<FavoriteCategory> {
}