package com.walter.starry.security.service;

import com.walter.starry.authorization.server.app.AuthorizationServerApplication;
import com.walter.starry.security.base.bo.AuthorityItemBo;
import com.walter.starry.security.base.service.AuthorityItemService;
import com.walter.starry.security.base.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = AuthorizationServerApplication.class)
public class AuthorityItemServiceTest {
    @Autowired
    private AuthorityItemService authorityItemService;

    @Test
    void getAllAuthorityTrees() throws Exception {
        List<AuthorityItemBo> list = authorityItemService.getAllAuthorityTrees(AuthorityItemBo.class, null);
        System.out.println(JsonUtil.toJson(list));
    }
}
