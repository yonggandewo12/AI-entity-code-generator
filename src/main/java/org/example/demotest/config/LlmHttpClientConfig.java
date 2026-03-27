package org.example.demotest.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP client setup for LLM requests.
 *
 * @author Liang.Xu
 */
@Configuration
public class LlmHttpClientConfig {

    /**
     * Configures RestTemplate for LLM API requests with custom timeout settings.
     * Uses timeout value from LlmProperties configuration.
     *
     * @param builder Spring RestTemplateBuilder
     * @param properties LLM configuration properties
     * @return Configured RestTemplate instance for LLM requests
     */
    @Bean
    public RestTemplate llmRestTemplate(RestTemplateBuilder builder, LlmProperties properties) {
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }
}
