package com.Auroral.blog.dao;

import com.Auroral.blog.entity.FavoriteTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

/**
 * 收藏网站标签Mapper
 *
 * @author Auroral
 */
@Repository
public interface FavoriteTagMapper extends BaseMapper<FavoriteTag> {
}