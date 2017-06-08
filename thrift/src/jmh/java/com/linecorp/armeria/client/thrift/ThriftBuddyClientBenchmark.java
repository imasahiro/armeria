package com.linecorp.armeria.client.thrift;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.ClassRule;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import com.linecorp.armeria.client.Client;
import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.ClientOptions;
import com.linecorp.armeria.client.DefaultClientBuilderParams;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.RpcRequest;
import com.linecorp.armeria.common.RpcResponse;
import com.linecorp.armeria.common.Scheme;
import com.linecorp.armeria.common.SerializationFormat;
import com.linecorp.armeria.common.thrift.ThriftCompletableFuture;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.thrift.THttpService;
import com.linecorp.armeria.service.test.thrift.main.HelloService;
import com.linecorp.armeria.service.test.thrift.main.HelloService.AsyncIface;
import com.linecorp.armeria.testing.server.ServerRule;

/**
 * Compare performance of proxy-based vs byte-buddy based {@link THttpClientInvocationHandler}.
 *
 * <p>20170511 Macbook Pro 2016 2.9 GHz Intel Core i5
 * <pre>
 * # Run complete. Total time: 00:19:32
 *
 * Benchmark                                                    Mode  Cnt     Score    Error  Units
 * c.l.a.client.thrift.ThriftBuddyClientBenchmark.buddy        thrpt  200  6621.462 ± 53.006  ops/s
 * c.l.a.client.thrift.ThriftBuddyClientBenchmark.proxy        thrpt  200  6562.484 ± 70.155  ops/s
 * </pre>
 */
@State(Scope.Benchmark)
public class ThriftBuddyClientBenchmark {
    @ClassRule
    public static final ServerRule server = new ServerRule() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.service("/hello", THttpService.of(
                    (AsyncIface) (name, resultHandler) -> resultHandler.onComplete(name)));
        }
    };

    private HelloService.Iface proxyClient;
    private HelloService.AsyncIface proxyClientAsync;
    private HelloService.Iface buddyClient;
    private HelloService.AsyncIface buddyClientAsync;

    @Setup
    public void startServer() {
        server.start();

        proxyClient = new ProxyTHttpClientFactory(ClientFactory.DEFAULT).newClient(
                "tbinary+h2c://127.0.0.1:" + server.httpPort() + "/hello", HelloService.Iface.class);
        proxyClientAsync = new ProxyTHttpClientFactory(ClientFactory.DEFAULT).newClient(
                "tbinary+h2c://127.0.0.1:" + server.httpPort() + "/hello", HelloService.AsyncIface.class);
        buddyClient = new THttpClientFactory(ClientFactory.DEFAULT).newClient(
                "tbinary+h2c://127.0.0.1:" + server.httpPort() + "/hello", HelloService.Iface.class);
        buddyClientAsync = new THttpClientFactory(ClientFactory.DEFAULT).newClient(
                "tbinary+h2c://127.0.0.1:" + server.httpPort() + "/hello", HelloService.AsyncIface.class);
    }

    @TearDown
    public void stopServer() throws Exception {
        server.stop().join();
    }

    @Benchmark
    public void proxy(Blackhole bh) throws Exception {
        bh.consume(proxyClient.hello("hello"));
    }

    @Benchmark
    public void proxyAsync(Blackhole bh) throws Exception {
        ThriftCompletableFuture<String> future = new ThriftCompletableFuture<>();
        proxyClientAsync.hello("hello", future);
        bh.consume(future.get());
    }

    @Benchmark
    public void buddy(Blackhole bh) throws Exception {
        bh.consume(buddyClient.hello("hello"));
    }

    @Benchmark
    public void buddyAsync(Blackhole bh) throws Exception {
        ThriftCompletableFuture<String> future = new ThriftCompletableFuture<>();
        buddyClientAsync.hello("hello", future);
        bh.consume(future.get());
    }

    static class ProxyTHttpClientFactory extends THttpClientFactory {
        ProxyTHttpClientFactory(ClientFactory httpClientFactory) {
            super(httpClientFactory);
        }

        @Override
        public <T> T newClient(URI uri, Class<T> clientType, ClientOptions options) {
            final Scheme scheme = validateScheme(uri);
            final SerializationFormat serializationFormat = scheme.serializationFormat();

            final Client<RpcRequest, RpcResponse> delegate = options.decoration().decorate(
                    RpcRequest.class, RpcResponse.class,
                    new THttpClientDelegate(newHttpClient(uri, scheme, options),
                                            serializationFormat));

            if (clientType == THttpClient.class) {
                // Create a THttpClient with path.
                @SuppressWarnings("unchecked")
                final T client = (T) new DefaultTHttpClient(
                        new DefaultClientBuilderParams(this, uri, THttpClient.class, options),
                        delegate, scheme.sessionProtocol(), newEndpoint(uri));
                return client;
            } else {
                // Create a THttpClient without path.
                final THttpClient thriftClient = new DefaultTHttpClient(
                        new DefaultClientBuilderParams(this, pathlessUri(uri), THttpClient.class, options),
                        delegate, scheme.sessionProtocol(), newEndpoint(uri));

                @SuppressWarnings("unchecked")
                T client = (T) Proxy.newProxyInstance(
                        clientType.getClassLoader(),
                        new Class<?>[] { clientType },
                        new ProxiedTHttpClientInvocationHandler(
                                new DefaultClientBuilderParams(this, uri, clientType, options),
                                thriftClient,
                                firstNonNull(uri.getRawPath(), "/"),
                                uri.getFragment()));
                return client;
            }
        }

        private Client<HttpRequest, HttpResponse> newHttpClient(URI uri, Scheme scheme, ClientOptions options) {
            try {
                @SuppressWarnings("unchecked")
                Client<HttpRequest, HttpResponse> client = delegate().newClient(
                        new URI(Scheme.of(SerializationFormat.NONE, scheme.sessionProtocol()).uriText(),
                                uri.getRawAuthority(), null, null, null),
                        Client.class, options);
                return client;
            } catch (URISyntaxException e) {
                throw new Error(e); // Should never happen.
            }
        }

        private static URI pathlessUri(URI uri) {
            try {
                return new URI(uri.getScheme(), uri.getRawAuthority(), null, null, null);
            } catch (URISyntaxException e) {
                throw new Error(e); // Should never happen.
            }
        }
    }
}
