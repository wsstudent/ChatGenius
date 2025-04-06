package com.abin.mallchat.common.user.domain.vo.request.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("添加用户请求")
public class AddUserReq {

    @NotBlank(message = "用户名不能为空")
    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("密码(可选，为空时使用默认密码)")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @ApiModelProperty("昵称")
    private String name;

    @ApiModelProperty("头像")
    private String avatar;

    @NotNull(message = "性别不能为空")
    @ApiModelProperty("性别 1为男性，2为女性")
    private Integer sex;
}