package com.wolfjc.thrift.example;

import com.wolfjc.thrift.core.annotation.ThriftService;

@ThriftService
public class HelloService {



    public String sayHello(){
        return "hello";
    }


}
