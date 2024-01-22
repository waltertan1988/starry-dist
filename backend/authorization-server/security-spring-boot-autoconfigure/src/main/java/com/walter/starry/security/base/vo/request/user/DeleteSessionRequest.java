package com.walter.starry.security.base.vo.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @author walter.tan
 */
@Data
public class DeleteSessionRequest {

    @NotBlank(message = "用户账号不能为空")
    private String username;

    @NotEmpty(message = "SessionId列表不能为空")
    private List<String> sessionIds;
}
