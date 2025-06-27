package com.walter.starry.security.base.common.message;

import com.walter.starry.security.base.util.MdcUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: walter.tan
 * @DateTime: 2025-06-27 15:37:47
 */
@Data
@NoArgsConstructor
public class MdcMessageWrapper {
    private String traceId;

    private String message;

    public MdcMessageWrapper(String message) {
        this.traceId = MdcUtil.getTraceId();
        this.message = message;
    }
}
