package com.walter.starry.security;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.walter.starry.authorization.server.app.AuthorizationServerApplication;
import com.walter.starry.security.base.entity.AclUser2;
import com.walter.starry.security.base.entity.AclUser2Example;
import com.walter.starry.security.base.mapper.AclUser2Mapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @Author: walter.tan
 * @DateTime: 2024-12-25 17:24:12
 */
@SpringBootTest(classes = AuthorizationServerApplication.class)
public class MyBatisTest {

    Logger log = LoggerFactory.getLogger(MyBatisTest.class);

    @Autowired
    private AclUser2Mapper aclUser2Mapper;

    @Test
    void selectByExampleAndPagination(){
        try(Page<?> ignored = PageHelper.startPage(1, 1)){
            AclUser2Example example = new AclUser2Example();
            example.createCriteria().andNicknameLike("%员%");
            example.setOrderByClause("id desc");
            List<AclUser2> userList = aclUser2Mapper.selectByExample(example);
            PageInfo<AclUser2> info = new PageInfo<>(userList);

            log.info("当前页：{}", info.getPageNum());
            log.info("总记录数：{}", info.getTotal());
            log.info("每页记录数：{}", info.getPageSize());
            log.info("总页数：{}", info.getPages());
            log.info("下一页：{}",info.getNextPage());
            log.info("上一页：{}",info.getPrePage());
            log.info("是否第一页：{}", info.isIsFirstPage());
            log.info("是否有下一页：{}", info.isHasNextPage());
            log.info("是否最末页：{}", info.isIsLastPage());
            log.info("是否有上一页：{}", info.isHasPreviousPage());
            log.info("连续显示的导航页：{}", info.getNavigatepageNums());
        }
    }
}
