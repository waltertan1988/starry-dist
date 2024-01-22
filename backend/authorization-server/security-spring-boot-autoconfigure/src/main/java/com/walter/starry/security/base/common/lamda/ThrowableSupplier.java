package com.walter.starry.security.base.common.lamda;


import com.walter.starry.security.base.common.exception.BizException;

/**
 * @author walter.tan
 */
@FunctionalInterface
public interface ThrowableSupplier<T> {
    T get() throws BizException;
}
