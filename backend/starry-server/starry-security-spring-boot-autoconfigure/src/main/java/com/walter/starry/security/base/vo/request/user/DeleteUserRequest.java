package com.walter.starry.security.base.vo.request.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @author walter.tan
 */
@Data
public class DeleteUserRequest {

    @NotEmpty(message = "账号列表不能为空")
    private List<String> usernameList;
}
