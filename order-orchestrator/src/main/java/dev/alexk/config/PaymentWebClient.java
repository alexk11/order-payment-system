package dev.alexk.config;


import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.util.concurrent.TimeUnit;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;


@Configuration
public class PaymentWebClient {

    @Value("${payment.stub.base-url}")
    private String baseUrl;

    @Value("${payment.stub.connect-timeout}")
    private int connectTimeout;

    @Value("${payment.stub.read-timeout}")
    private int readTimeout;

    @Bean
    public WebClient webClientWithTimeout() {
        final var httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .doOnConnected(connection -> {
                    connection.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.SECONDS));
                    connection.addHandlerLast(new WriteTimeoutHandler(readTimeout, TimeUnit.SECONDS));
                });

        return WebClient.builder().baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }

}
