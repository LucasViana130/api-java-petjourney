package br.com.fiap.petjourney.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class SmartCareClientConfig {

    @Bean("smartCareRestClient")
    RestClient smartCareRestClient(
            RestClientBuilderConfigurer builderConfigurer,
            @Value("${smartcare.base-url}") String baseUrl,
            @Value("${smartcare.connect-timeout}") Duration connectTimeout,
            @Value("${smartcare.read-timeout}") Duration readTimeout
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return builderConfigurer.configure(RestClient.builder())
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
