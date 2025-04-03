package com.abin.mallchat.common.user.domain.vo.request.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("删除用户请求")
public class DeleteUserReq {

    @NotNull(message = "用户ID不能为空")
    @ApiModelProperty("用户ID")
    private Long id;
}