package com.wolfjc.thrift.core.annotation;


import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * thrift服务注解
 *
 * 用来标记thrift服务
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Component
public @interface ThriftService {

    /**
     * 服务名称
     *
     * @return
     */
    String name();


}
