package com.linecorp.armeria.example.http.thrift;

import javax.inject.Named;

import org.apache.thrift.TException;

import com.linecorp.armeria.example.http.thrift.ThriftOverHttpServer.ArmeriaServer;
import com.linecorp.armeria.service.sample.thrift.HelloService;

@Named
@ArmeriaServer
public class HelloThriftAPISyncHandler implements HelloService.Iface {
    @Override
    public String hello(String name) throws TException {
        return "Hello " + name;
    }
}
