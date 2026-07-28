package com.Auroral.blog.vo;

import com.Auroral.blog.annotation.ValidAccount;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * @author Auroral
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "用户注册")
public class UserVO {
    /**
     * 用户名（可以是邮箱或手机号）
     */
    @NotBlank(message = "账号不能为空")
    @ValidAccount(message = "账号格式不正确")
    @ApiModelProperty(name = "username", value = "用户名（邮箱或手机号）", required = true, dataType = "String")
    private String username;

    /**
     * 密码
     */
    @Size(min = 6, message = "密码不能少于6位")
    @NotBlank(message = "密码不能为空")
    @ApiModelProperty(name = "password", value = "密码", required = true, dataType = "String")
    private String password;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    @ApiModelProperty(name = "code", value = "验证码", required = true, dataType = "String")
    private String code;
}