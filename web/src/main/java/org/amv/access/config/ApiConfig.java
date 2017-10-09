package org.amv.access.config;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.auth.ApplicationAuthenticationArgumentResolver;
import org.amv.access.api.auth.ApplicationAuthenticationArgumentResolver.ApiKeyResolver;
import org.amv.access.api.auth.NonceAuthenticationArgumentResolver;
import org.amv.access.model.Application;
import org.amv.access.model.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Slf4j
@Configuration
public class ApiConfig extends WebMvcConfigurerAdapter {

    private final ApplicationRepository applicationRepository;

    @Autowired
    public ApiConfig(ApplicationRepository applicationRepository) {
        this.applicationRepository = requireNonNull(applicationRepository);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(authenticationArgumentResolver());
        argumentResolvers.add(nonceAuthenticationArgumentResolver());
    }

    @Bean
    public ApiKeyResolver apiKeyResolver() {
        return apiKey -> {
            try {
                Optional<Application> application = applicationRepository.findOneByApiKey(apiKey);
                return Mono.justOrEmpty(application);
            } catch (Exception e) {
                return Mono.error(e);
            }
        };
    }

    @Bean
    public ApplicationAuthenticationArgumentResolver authenticationArgumentResolver() {
        return new ApplicationAuthenticationArgumentResolver(apiKeyResolver());
    }

    @Bean
    public NonceAuthenticationArgumentResolver nonceAuthenticationArgumentResolver() {
        return new NonceAuthenticationArgumentResolver();
    }
}
