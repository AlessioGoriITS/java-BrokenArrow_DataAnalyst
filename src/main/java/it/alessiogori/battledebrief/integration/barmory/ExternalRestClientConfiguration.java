package it.alessiogori.battledebrief.integration.barmory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
@EnableConfigurationProperties(ExternalHttpProperties.class)
public class ExternalRestClientConfiguration {

    @Bean
    RestClientCustomizer externalHttpTimeoutCustomizer(
            ExternalHttpProperties properties
    ) {
        return builder -> {
            SimpleClientHttpRequestFactory requestFactory =
                    new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(properties.connectTimeout());
            requestFactory.setReadTimeout(properties.readTimeout());
            builder.requestFactory(requestFactory);
        };
    }
}
