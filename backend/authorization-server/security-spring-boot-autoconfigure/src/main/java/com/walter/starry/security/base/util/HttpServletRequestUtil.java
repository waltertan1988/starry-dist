package com.walter.starry.security.base.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

import java.util.Iterator;

/**
 * @Author: walter.tan
 * @DateTime: 2023-11-20 10:30:27
 */
@UtilityClass
public class HttpServletRequestUtil {

    /**
     * 判断请求是否为AJAX请求
     * @param request
     * @return
     */
    public static boolean isAjaxRequest(HttpServletRequest request){
        Iterator<String> it = request.getHeaders("X-Requested-With").asIterator();
        while(it.hasNext()){
            if("XMLHttpRequest".equals(it.next())){
                return true;
            }
        }
        return false;
    }
}
