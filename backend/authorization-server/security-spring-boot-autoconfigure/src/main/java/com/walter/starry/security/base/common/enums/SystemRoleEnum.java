package com.walter.starry.security.base.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.Authentication;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 系统固定角色枚举
 * @Author: walter.tan
 * @DateTime: 2023-10-12 16:55:36
 */
@Getter
@AllArgsConstructor
public enum SystemRoleEnum {

    ROLE_ADMIN("系统管理员"),
    ROLE_ANONYMOUS("匿名用户"),
    ROLE_USER("已登录用户"),
    ;

    private final String desc;

    public static boolean isAdmin(Authentication authentication){
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.name().equals(authority.getAuthority()));
    }

    public static Set<String> getSystemRoleCodes(){
        return Stream.of(SystemRoleEnum.values()).map(Enum::name).collect(Collectors.toUnmodifiableSet());
    }
}
