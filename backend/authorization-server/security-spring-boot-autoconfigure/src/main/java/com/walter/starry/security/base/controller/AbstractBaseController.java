package com.walter.starry.security.base.controller;

import com.google.common.collect.Lists;
import com.walter.starry.security.base.vo.response.ApiResponse;
import com.walter.starry.security.base.common.exception.BizException;
import com.walter.starry.security.base.common.lamda.ThrowableSupplier;
import com.walter.starry.security.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.List;
import java.util.Objects;

/**
 * @author walter.tan
 */
@Slf4j
public abstract class AbstractBaseController {

    /**
     * Controller API的通用调用
     * @param logPrefix 日志前缀
     * @param bindingResult BindingResult
     * @param supplier 处理业务的逻辑
     * @param <T> 待返回的数据类型
     * @return
     */
    protected <T> ApiResponse<T> apiCall(String logPrefix, BindingResult bindingResult, ThrowableSupplier<T> supplier){
        try{
            if(Objects.nonNull(bindingResult) && bindingResult.hasErrors()){
                List<String> errMsgList = Lists.newArrayList();

                for (ObjectError err : bindingResult.getAllErrors()) {
                    if(err instanceof FieldError){
                        Object rejVal = ((FieldError) err).getRejectedValue();
                        if(Objects.isNull(rejVal)) {
                            errMsgList.add(err.getDefaultMessage());
                        }else if(rejVal instanceof String && StringUtils.isEmpty((String)rejVal)) {
                            errMsgList.add(err.getDefaultMessage());
                        }else{
                            errMsgList.add(err.getDefaultMessage() + ": " + rejVal);
                        }
                    }else{
                        errMsgList.add(err.getDefaultMessage());
                    }
                }

                throw new BizException(ApiResponse.ErrCode.BAD_REQUEST, JsonUtil.toJson(errMsgList));
            }

            T result = supplier.get();

            return ApiResponse.success(result);
        } catch (BizException ex){
            log.warn(logPrefix + " failed by BizException, code={}, msg={}", ex.getCode().getValue(), ex.getMessage(), ex);
            return ApiResponse.fail(ex.getCode(), ex.getMessage());
        } catch (Exception ex){
            log.error(logPrefix + " error by Exception", ex);
            return ApiResponse.fail(ApiResponse.ErrCode.INTERNAL_SERVER_ERROR, "系统错误");
        }
    }
}
