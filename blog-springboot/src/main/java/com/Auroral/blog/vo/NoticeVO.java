package com.Auroral.blog.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 公告视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "公告")
public class NoticeVO {
    
    /**
     * 公告id
     */
    @ApiModelProperty(name = "id", value = "公告id", dataType = "Integer")
    private Integer id;
    
    /**
     * 公告内容
     */
    @NotBlank(message = "公告内容不能为空")
    @ApiModelProperty(name = "content", value = "公告内容", required = true, dataType = "String")
    private String content;
}