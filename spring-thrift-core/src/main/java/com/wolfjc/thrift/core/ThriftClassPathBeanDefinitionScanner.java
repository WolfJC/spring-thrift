package com.wolfjc.thrift.core;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import java.util.Set;

/**
 * 自定义注解扫描器
 *
 * 提供了一个禁用默认过滤器的构造函数
 *
 * 扫描{@link com.wolfjc.thrift.core.annotation.ThriftService}注解
 */
public class ThriftClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    /**
     *
     * @param registry
     * @param useDefaultFilters  若为true，则会扫描{@link org.springframework.stereotype.Component}注解，以及包含该注解的其他注解
     * @param environment
     * @param resourceLoader
     */
    public ThriftClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters, Environment environment, ResourceLoader resourceLoader) {
        super(registry, useDefaultFilters, environment, resourceLoader);
    }

    public ThriftClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, Environment environment, ResourceLoader resourceLoader) {
        this(registry,false,environment,resourceLoader);
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        return super.doScan(basePackages);
    }

    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
        return super.checkCandidate(beanName, beanDefinition);
    }
}
