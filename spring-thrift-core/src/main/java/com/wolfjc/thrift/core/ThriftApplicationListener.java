package com.wolfjc.thrift.core;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * 监听event，启用或者停用Thrift服务
 */
public class ThriftApplicationListener implements ApplicationListener<ApplicationEvent> {

    /**
     *  Thrift服务启动类
     */
    private ThriftBootstrap thriftBootstrap;

    public ThriftApplicationListener() {
        this.thriftBootstrap = new ThriftBootstrap();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ContextRefreshedEvent) {
            thriftBootstrap.start();
        } else if (applicationEvent instanceof ContextClosedEvent) {
          thriftBootstrap.stop();
        }
    }
}
