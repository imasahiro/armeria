package com.linecorp.armeria.example.http.thrift;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Named;

import org.apache.thrift.TException;

import com.linecorp.armeria.example.http.thrift.ThriftOverHttpServer.ArmeriaClient;
import com.linecorp.armeria.server.http.dynamic.Converter;
import com.linecorp.armeria.server.http.dynamic.DynamicHttpService;
import com.linecorp.armeria.server.http.dynamic.Get;
import com.linecorp.armeria.server.http.dynamic.Path;
import com.linecorp.armeria.server.http.dynamic.PathParam;
import com.linecorp.armeria.service.sample.thrift.HelloService;

@Named
@Converter(target = String.class, value = JacksonResponseConverter.class)
public class HelloRestAPIHandler extends DynamicHttpService {
    private final HelloService.Iface syncClient;

    public HelloRestAPIHandler(@ArmeriaClient HelloService.Iface syncClient) {
        this.syncClient = syncClient;
    }

    @Get
    @Path("/hello/{name}")
    public CompletionStage<String> hello(@PathParam("name") String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return syncClient.hello(name);
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
