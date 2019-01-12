package com.wolfjc.thrift.example.service.impl;

import com.wolfjc.thrift.core.annotation.ThriftService;
import com.wolfjc.thrift.example.service.CalculatorService;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThriftService(name = "calculatorService")
public class CalculatorServiceImpl implements CalculatorService.Iface {

    private Logger logger = LoggerFactory.getLogger(CalculatorServiceImpl.class);

    @Override
    public int add(int num1, int num2) throws TException {
        logger.info("[CalculatorServiceImpl][IN]:num1 {} ,num2 {}",num1,num2);
        int result =  num1 + num2;
        logger.info("[CalculatorServiceImpl][OUT]:{}",result);
        return result;
    }
}
