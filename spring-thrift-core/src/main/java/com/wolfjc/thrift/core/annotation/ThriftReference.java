package com.wolfjc.thrift.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Thrift服务引用
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ThriftReference {

    /**
     * 引用的服务名称
     *
     * @return
     */
    String name() default "";
}
