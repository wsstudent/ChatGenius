// 新建 ModifyAvatarReq.java
package com.abin.mallchat.common.user.domain.vo.request.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * 修改用户头像请求
 */
@Data
public class ModifyAvatarReq {

    @NotBlank
    @ApiModelProperty("头像URL地址")
    private String avatar;
}