package com.wolfjc.thrift.core.context;


import com.wolfjc.thrift.core.ThriftScannerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 扫描自定义组件注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(value = ThriftScannerRegistrar.class)
public @interface ThriftComponentScan {

    String[] value() default {};

    /**
     * 扫描路径
     *
     * @return
     */
    String[] basePackages() default {};
}
