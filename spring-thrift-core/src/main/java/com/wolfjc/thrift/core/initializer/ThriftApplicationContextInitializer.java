package com.wolfjc.thrift.core.initializer;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 *  ApplicationContext初始化时注册Thrift监听器
 */
public class ThriftApplicationContextInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

        configurableApplicationContext.addApplicationListener(new ThriftApplicationListener());
    }
}
