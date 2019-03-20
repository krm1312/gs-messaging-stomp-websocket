package hello;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private static final String USER = "test";
    private static final String PASS = "test";

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        StompBrokerRelayRegistration relayRegistration = config.enableStompBrokerRelay("/topic", "/queue")
            .setRelayHost("127.0.0.1")
            .setRelayPort(13354)
            .setClientLogin(USER)
            .setSystemLogin(USER)
            .setSystemPasscode(PASS)
            .setClientPasscode(PASS);

        relayRegistration.setTcpClient(create("127.0.0.1", 13354, true, "TLSv1.2", true));

        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/gs-guide-websocket").withSockJS();
    }


    private ReactorNettyTcpClient create(String host, int port,
                                               boolean useTls, String tlsVersion,
                                               boolean ignoreInvalidCerts) {

        log.info("Configuring Stomp Client {}:{}, tls: {}", host, port, useTls);

        return new ReactorNettyTcpClient<>(options -> {
            if (!useTls) {
                return options.host(host).port(port);
            } else {
                try {
                    SslContextBuilder sslBuilder = SslContextBuilder.forClient();

                    if (!StringUtils.isEmpty(tlsVersion)) {
                        sslBuilder.protocols(tlsVersion);
                    }

                    if (ignoreInvalidCerts) {
                        sslBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
                    }
                    SslContext sslContext = sslBuilder.build();
                    return options
                        .host(host)
                        .port(port)
                        .secure(sslProviderBuilder -> sslProviderBuilder.sslContext(sslContext));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unable to establish sslAlgorithm for " + tlsVersion, e);
                }
            }

        }, new StompReactorNettyCodec());


    }

}