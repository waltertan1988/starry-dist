package com.walter.starry.security;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * @author: walter.tan
 * @datetime: 2023/9/17 10:21
 */
public class AntPathMatcherTest {

    @Test
    public void test(){
        PathMatcher matcher = new AntPathMatcher();

        String pattern = Lists.newArrayList(
                "/admin/**",
//                        "/admin/menu/l*",
                        "/admin/m*u/l*",
                        "/admin/menu/lis*/{id:[a-zA-Z0-9]+}/{subId:[a-zA-Z0-9]+}",
                        "/admin/menu/list/{id:[a-zA-Z0-9]+}/{subId:[a-zA-Z0-9]+}",
                        "/admin/menu/lis?/{id:[a-zA-Z0-9]+}/{subId:[a-zA-Z0-9]+}",
//                        "/admin/menu/lis*",
//                        "/admin/menu/lis?",
                        "/admin/menu/**",
                        "/**")
                .stream().min(matcher.getPatternComparator("/admin/menu/list/1/2?a=1&b=2"))
                .orElse(null);

        System.out.println(pattern);

    }
}
