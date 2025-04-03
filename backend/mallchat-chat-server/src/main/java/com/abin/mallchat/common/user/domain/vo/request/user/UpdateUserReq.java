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
@ApiModel("更新��户请求")
public class UpdateUserReq {

    @NotNull(message = "用户ID不能为空")
    @ApiModelProperty("用户ID")
    private Long id;

    @NotBlank(message = "昵称不能为空")
    @ApiModelProperty("昵称")
    private String name;

    @ApiModelProperty("头像")
    private String avatar;

    @NotNull(message = "性别不能为空")
    @ApiModelProperty("性别 1为男性，2为女性")
    private Integer sex;
}