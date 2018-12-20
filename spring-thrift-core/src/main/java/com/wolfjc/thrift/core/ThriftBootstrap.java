package com.wolfjc.thrift.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ThriftBootstrap {

    private Log log = LogFactory.getLog(ThriftBootstrap.class);

    /**
     * 启动Thrift服务
     */
    public void start(){
        log.info("Thrift Service start");
    }

    /**
     * 停用Thrift服务
     */
    public void stop(){
        log.info("Thrift Service stop");
    }
}
