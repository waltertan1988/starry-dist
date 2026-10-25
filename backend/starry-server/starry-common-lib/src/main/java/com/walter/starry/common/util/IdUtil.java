package com.walter.starry.common.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: walter.tan
 * @DateTime: 2024-03-18 15:18:28
 */
@UtilityClass
public class IdUtil {

    public static String getUUID(){
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取下一个全局ID
     * @return
     */
    public static synchronized Long genNextGlobalId() {
        // TODO tyx 改用雪花算法Id
        try {
            TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return System.currentTimeMillis();
    }
}
