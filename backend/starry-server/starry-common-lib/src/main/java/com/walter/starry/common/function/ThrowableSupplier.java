package com.walter.starry.common.function;


import com.walter.starry.common.exception.BizException;

/**
 * @author walter.tan
 */
@FunctionalInterface
public interface ThrowableSupplier<T> {
    T get() throws BizException;
}
