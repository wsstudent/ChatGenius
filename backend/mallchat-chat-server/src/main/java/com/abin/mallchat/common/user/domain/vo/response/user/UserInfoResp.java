package com.abin.mallchat.common.user.domain.vo.response.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Description: 用户信息返回
 * Author: <a href="https://github.com/zongzibinbin">abin</a>
 * Date: 2023-03-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("用户详情")
public class UserInfoResp {

    @ApiModelProperty(value = "用户id")
    private Long id;

    @ApiModelProperty(value = "用户昵称")
    private String name;

    @ApiModelProperty(value = "用户头像")
    private String avatar;

    @ApiModelProperty(value = "性别 1为男性，2为女性")
    private Integer sex;

    @ApiModelProperty(value = "剩余改名次数")
    private Integer modifyNameChance;

    @ApiModelProperty("token")
    private String token;

}

