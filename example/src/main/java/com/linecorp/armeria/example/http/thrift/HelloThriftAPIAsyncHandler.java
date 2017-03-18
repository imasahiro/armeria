package com.linecorp.armeria.example.http.thrift;

import static com.linecorp.armeria.common.util.Functions.voidFunction;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.linecorp.armeria.common.thrift.ThriftCompletableFuture;
import com.linecorp.armeria.example.http.thrift.ThriftOverHttpServer.ArmeriaClient;
import com.linecorp.armeria.example.http.thrift.ThriftOverHttpServer.ArmeriaServer;
import com.linecorp.armeria.service.sample.thrift.HelloService;

@Named
@ArmeriaServer
public class HelloThriftAPIAsyncHandler implements HelloService.AsyncIface {

    private final HelloService.AsyncIface asyncClient;

    @Inject
    public HelloThriftAPIAsyncHandler(@ArmeriaClient HelloService.AsyncIface asyncClient) {
        this.asyncClient = asyncClient;
    }

    @Override
    public void hello(String name, AsyncMethodCallback<String> resultHandler) throws TException {
        ThriftCompletableFuture<String> future = new ThriftCompletableFuture<>();
        try {
            asyncClient.hello(name, future);
        } catch (TException e) {
            resultHandler.onError(e);
            return;
        }
        future.handle(voidFunction((result, thrown) -> {
            if (thrown != null) {
                resultHandler.onError((Exception) thrown);
            } else {
                resultHandler.onComplete(result);
            }
        }));
    }
}
