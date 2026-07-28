package com.Auroral.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收藏网站分类
 *
 * @author Auroral
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteCategoryDTO {

    /**
     * 分类id
     */
    private Integer id;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类图标
     */
    private String categoryIcon;
}