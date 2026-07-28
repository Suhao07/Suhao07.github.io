package com.Auroral.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 收藏网站
 *
 * @author Auroral
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteDTO {

    /**
     * 收藏id
     */
    private Integer id;

    /**
     * 网站名称
     */
    private String name;

    /**
     * 网站地址
     */
    private String url;

    /**
     * 网站描述
     */
    private String description;

    /**
     * 网站图标
     */
    private String icon;

    /**
     * 分类id
     */
    private Integer categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类图标
     */
    private String categoryIcon;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 排序
     */
    private Integer sort;
}