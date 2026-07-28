package com.Auroral.blog.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
@ApiModel(description = "收藏网站")
public class FavoriteVO {

    /**
     * 收藏id
     */
    @ApiModelProperty(name = "id", value = "收藏网站id", dataType = "Integer")
    private Integer id;

    /**
     * 网站名称
     */
    @NotBlank(message = "网站名称不能为空")
    @ApiModelProperty(name = "name", value = "网站名称", required = true, dataType = "String")
    private String name;

    /**
     * 网站地址
     */
    @NotBlank(message = "网站地址不能为空")
    @ApiModelProperty(name = "url", value = "网站地址", required = true, dataType = "String")
    private String url;

    /**
     * 网站描述
     */
    @ApiModelProperty(name = "description", value = "网站描述", dataType = "String")
    private String description;

    /**
     * 网站图标
     */
    @ApiModelProperty(name = "icon", value = "网站图标", dataType = "String")
    private String icon;

    /**
     * 分类id
     */
    @NotNull(message = "分类id不能为空")
    @ApiModelProperty(name = "categoryId", value = "分类id", required = true, dataType = "Integer")
    private Integer categoryId;

    /**
     * 排序
     */
    @ApiModelProperty(name = "sort", value = "排序", dataType = "Integer")
    private Integer sort;

    /**
     * 标签列表
     */
    @ApiModelProperty(name = "tags", value = "标签列表", dataType = "List<String>")
    private List<String> tags;
}