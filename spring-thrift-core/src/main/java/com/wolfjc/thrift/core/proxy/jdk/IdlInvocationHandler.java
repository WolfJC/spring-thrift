package com.wolfjc.thrift.core.proxy.jdk;

import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

public class IdlInvocationHandler implements InvocationHandler {

    private Logger logger = LoggerFactory.getLogger(IdlInvocationHandler.class);

    private Class<? extends TServiceClient> cl;

    private static final String SERVER_HOST = "192.168.0.119";

    private static final int SERVER_PORT = 9544;

    private static final int TIMEOUT_MILLS =0;

    public IdlInvocationHandler(Class<? extends TServiceClient> cl) {
        this.cl = cl;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Class<?> clazz = cl.getDeclaringClass();

        String serviceName = clazz.getName();

        TTransport transport = null;
        Object resp = null;
        try {
            transport = new TFramedTransport(new TSocket(SERVER_HOST, SERVER_PORT, TIMEOUT_MILLS));
            transport.open();
            TProtocol protocol = new TCompactProtocol(transport);
            TMultiplexedProtocol multiProtocol = new TMultiplexedProtocol(protocol, serviceName);
            Constructor<? extends TServiceClient> constructor = cl.getConstructor(TProtocol.class);
            TServiceClient instance = constructor.newInstance(multiProtocol);
            logger.info("invoking '{}.{}' at {}:{}", serviceName, method.getName(), SERVER_HOST, SERVER_PORT);
            logger.debug("params:{}", Arrays.toString(args));
            resp = method.invoke(instance, args);
            return resp;
        } catch (Exception e) {
            logger.error("fail to invoke {}.{} at {}:{}", serviceName, method.getName(), SERVER_HOST, SERVER_PORT);
            logger.error("params:{}", Arrays.toString(args));
            logger.error(e.getMessage(), e);
            throw e;
        } finally{
            if (transport != null)   {
                transport.close();
            }
        }
    }
}
