package com.walter.starry.security;

import com.walter.starry.authorization.server.app.AuthorizationServerApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.Date;

/**
 * @Author: walter.tan
 * @DateTime: 2024-02-01 09:39:37
 */
@SpringBootTest(classes = AuthorizationServerApplication.class)
public class RedissonTest {

    @Autowired
    private RedissonClient redissonClient;

    @Nested
    class RBucketTest {

        private static final String keyPrefix = "walter:test:bucket:";

        @Test
        void setGet(){
            // 字符串类型
            String stringVal = "hello";
            RBucket<String> stringBucket = redissonClient.getBucket(keyPrefix + "string");
            stringBucket.set(stringVal, Duration.ofSeconds(60));

            // 对象类型
            Person exp = new Person();
            exp.setId(7L);
            exp.setName("walter");
            exp.setBirthday(new Date());
            RBucket<Person> objectBucket = redissonClient.getBucket(keyPrefix + "object");
            objectBucket.set(exp, Duration.ofSeconds(60));

            // 测试断言
            Assertions.assertEquals(stringVal, stringBucket.get());
            Person act = objectBucket.get();
            Assertions.assertTrue(exp.id == act.id && exp.name.equals(act.name) && exp.birthday.equals(act.birthday));
        }

        public static class Person {
            private long id;
            private String name;
            private Date birthday;

            public long getId() {
                return id;
            }

            public void setId(long id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public Date getBirthday() {
                return birthday;
            }

            public void setBirthday(Date birthday) {
                this.birthday = birthday;
            }
        }
    }
}
