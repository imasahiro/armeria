package com.linecorp.armeria.example.http.thrift;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.codahale.metrics.MetricRegistry;

import com.linecorp.armeria.client.ClientBuilder;
import com.linecorp.armeria.client.logging.DropwizardMetricCollectingClient;
import com.linecorp.armeria.client.logging.LoggingClient;
import com.linecorp.armeria.common.RpcRequest;
import com.linecorp.armeria.common.RpcResponse;
import com.linecorp.armeria.common.http.HttpSessionProtocols;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.logging.DropwizardMetricCollectingService;
import com.linecorp.armeria.server.logging.LoggingService;
import com.linecorp.armeria.server.logging.SampledLoggingService;
import com.linecorp.armeria.server.thrift.THttpService;
import com.linecorp.armeria.service.sample.thrift.HelloService;

@SpringBootApplication
public class ThriftOverHttpServer {
    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    public @interface ArmeriaServer {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    public @interface ArmeriaClient {
    }

    private static final int port = 8080;

    @Bean
    MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Bean
    @ArmeriaClient
    HelloService.Iface helloSyncClient(MetricRegistry metricRegistry) {
        return new ClientBuilder("tbinary+http://127.0.0.1:" + port + "/thrift/async")
                .decorator(RpcRequest.class, RpcResponse.class,
                           DropwizardMetricCollectingClient.newDecorator(metricRegistry, "HelloSyncClient"))
                .decorator(RpcRequest.class, RpcResponse.class, LoggingClient::new)
                .build(HelloService.Iface.class);
    }

    @Bean
    @ArmeriaClient
    HelloService.AsyncIface helloAsyncClient(MetricRegistry metricRegistry) {
        return new ClientBuilder("tbinary+http://127.0.0.1:" + port + "/thrift/sync")
                .decorator(RpcRequest.class, RpcResponse.class,
                           DropwizardMetricCollectingClient.newDecorator(metricRegistry, "HelloSyncClient"))
                .decorator(RpcRequest.class, RpcResponse.class, LoggingClient::new)
                .build(HelloService.AsyncIface.class);
    }

    @Bean
    Server server(@ArmeriaServer HelloService.AsyncIface asyncHandler,
                  @ArmeriaServer HelloService.Iface syncHandler,
                  HelloRestAPIHandler restHandler,
                  MetricRegistry metricRegistry) {
        ServerBuilder sb = new ServerBuilder()
                .serviceAt("/thrift/sync",
                           THttpService.of(syncHandler)
                                       .decorate(SampledLoggingService.newDecorator()))
                .serviceAt("/thrift/async",
                           THttpService.of(asyncHandler)
                                       .decorate(DropwizardMetricCollectingService.newDecorator(
                                               metricRegistry, "HelloAsyncService"))
                                       .decorate(LoggingService::new))
                .serviceUnder("/api", restHandler.decorate(LoggingService::new))
                .port(8080, HttpSessionProtocols.HTTP);
        Server server = sb.build();
        server.start().join();
        return server;
    }

    public static void main(String[] args) {
        SpringApplication.run(ThriftOverHttpServer.class, args);
    }
}
