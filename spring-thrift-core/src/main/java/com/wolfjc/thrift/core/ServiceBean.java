package com.wolfjc.thrift.core;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.concurrent.ConcurrentHashMap;

/**
 * ServiceBean
 */
@Deprecated
public class ServiceBean implements InitializingBean, DisposableBean, ApplicationListener<ContextRefreshedEvent> {


    private ConcurrentHashMap<String,Object> map = new ConcurrentHashMap<>();


    private Object ref;


    /**
     * 监听 {@link ContextRefreshedEvent} 事件
     * @param event
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        //暴露服务
        export();
    }

    //todo::如何暴露服务
    private void export(){

    }

    /**
     * bean的初始化
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {

    }

    /**
     * bean的销毁
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {

    }


    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }
}
