package com.wolfjc.thrift.core;

import com.wolfjc.thrift.core.context.ThriftComponentScan;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

/**
 *
 * 参考dubbo的DubboComponentScanRegistrar以及MyBatis的MapperScannerRegistrar
 */
public class ThriftScannerRegistrar implements ImportBeanDefinitionRegistrar{

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        //获取扫描的包路径
        Set<String> packagesToScan = getPackagesToScan(annotationMetadata);

        registerServiceBeanPostProcessor(packagesToScan,beanDefinitionRegistry);

        registerReferenceBeanPostProcessor(beanDefinitionRegistry);

    }


    private Set<String> getPackagesToScan(AnnotationMetadata annotationMetadata){

        Map<String, Object> metadataAnnotationAttributes = annotationMetadata.getAnnotationAttributes(ThriftComponentScan.class.getName());

        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(metadataAnnotationAttributes);

        String[] basePackages = annotationAttributes.getStringArray("basePackages");

        String[] value = annotationAttributes.getStringArray("value");

        Set<String> allPackages = new LinkedHashSet<>(Arrays.asList(value));

        allPackages.addAll(Arrays.asList(basePackages));

        if (CollectionUtils.isEmpty(allPackages)){
            //扫描注解类所在包以及包下的所有类
            return Collections.singleton(ClassUtils.getPackageName(annotationMetadata.getClassName()));
        }
        return allPackages;
    }

    /**
     * 注入bean   thriftServiceBeanPostProcessor
     * @param packagesToScan
     * @param registry
     */
    private void registerServiceBeanPostProcessor(Set<String> packagesToScan,BeanDefinitionRegistry registry){
        BeanDefinitionBuilder builder = rootBeanDefinition(ThriftServiceBeanPostProcessor.class);
        builder.addConstructorArgValue(packagesToScan);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        registry.registerBeanDefinition("thriftServiceBeanPostProcessor",beanDefinition);
    }

    /**
     * 注入bean  thriftReferenceBeanPostProcessor
     * @param beanDefinitionRegistry
     */
    private void registerReferenceBeanPostProcessor(BeanDefinitionRegistry beanDefinitionRegistry){
        if (!beanDefinitionRegistry.containsBeanDefinition("thriftReferenceBeanPostProcessor")) {
            RootBeanDefinition beanDefinition = new RootBeanDefinition(ThriftReferenceBeanPostProcessor.class);
            beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            beanDefinitionRegistry.registerBeanDefinition("thriftReferenceBeanPostProcessor", beanDefinition);
        }
    }
}
