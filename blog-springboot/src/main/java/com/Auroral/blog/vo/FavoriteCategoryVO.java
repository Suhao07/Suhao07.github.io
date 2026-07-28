package com.Auroral.blog.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 收藏网站分类
 *
 * @author Auroral
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "收藏网站分类")
public class FavoriteCategoryVO {

    /**
     * 分类id
     */
    @ApiModelProperty(name = "id", value = "分类id", dataType = "Integer")
    private Integer id;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @ApiModelProperty(name = "categoryName", value = "分类名称", required = true, dataType = "String")
    private String categoryName;

    /**
     * 分类图标
     */
    @ApiModelProperty(name = "categoryIcon", value = "分类图标", dataType = "String")
    private String categoryIcon;

    /**
     * 排序
     */
    @ApiModelProperty(name = "sort", value = "排序", dataType = "Integer")
    private Integer sort;
}