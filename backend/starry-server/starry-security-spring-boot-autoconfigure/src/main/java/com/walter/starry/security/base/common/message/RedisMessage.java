package com.walter.starry.security.base.common.message;

import com.walter.starry.common.util.MdcUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @Author: walter.tan
 * @DateTime: 2025-06-27 15:37:47
 */
@Data
@NoArgsConstructor
public class RedisMessage {

    private String msgId;

    private String traceId;

    private String body;

    public RedisMessage(String body) {
        this.msgId = UUID.randomUUID().toString().replace("-", "");
        this.traceId = MdcUtil.getTraceId();
        this.body = body;
    }
}
