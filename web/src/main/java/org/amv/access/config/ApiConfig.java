package org.amv.access.config;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.auth.ApplicationAuthenticationArgumentResolver;
import org.amv.access.api.auth.ApplicationAuthenticationArgumentResolver.ApplicationResolver;
import org.amv.access.api.auth.NonceAuthenticationArgumentResolver;
import org.amv.access.exception.NotFoundException;
import org.amv.access.exception.UnprocessableEntityException;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.model.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import reactor.core.publisher.Mono;

import java.util.List;

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
    public ApplicationResolver apiKeyResolver() {
        return (appId, apiKey) -> {
            try {
                ApplicationEntity application = applicationRepository.findOneByAppId(appId)
                        .orElseThrow(() -> new NotFoundException("ApplicationEntity not found"));

                boolean isMatchingApiKey = apiKey.equals(application.getApiKey());
                if (!isMatchingApiKey) {
                    // do not expose information about existing applications, hence:  NotFoundException
                    throw new NotFoundException("ApplicationEntity not found");
                }

                if (!application.isEnabled()) {
                    throw new UnprocessableEntityException("ApplicationEntity with given appId is disabled");
                }

                return Mono.just(application);
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
