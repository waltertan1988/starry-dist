package com.walter.starry.business.app.controller;

import com.walter.starry.common.util.JsonUtil;
import com.walter.starry.security.base.vo.request.user.ListUserRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Slf4j
public class UserControllerTest extends AbstractControllerJupiterTest{

    @Test
    @SneakyThrows
    void list(){
        ListUserRequest req = new ListUserRequest();

        MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build()
                .perform(
                    MockMvcRequestBuilders
                        .post("/admin/user/list")
                        .header("Cookie", "SESSION=MzQ0OGQxYjgtYjU1ZC00YTU3LWJhMzUtM2M2ZjJlOTI0N2Y1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(req))
        ).andDo(MockMvcResultHandlers.print());
    }
}
