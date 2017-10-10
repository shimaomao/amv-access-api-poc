package org.amv.access.api.auth;

import com.google.common.base.CharMatcher;
import com.google.common.net.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.ApplicationAuthentication;
import org.amv.access.auth.ApplicationAuthenticationImpl;
import org.amv.access.core.Application;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.NotFoundException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@Slf4j
public class ApplicationAuthenticationArgumentResolver implements HandlerMethodArgumentResolver {

    public interface ApiKeyResolver extends Function<String, Mono<Application>> {

    }

    private ApiKeyResolver apiKeyResolver;

    public ApplicationAuthenticationArgumentResolver(ApiKeyResolver apiKeyResolver) {
        this.apiKeyResolver = requireNonNull(apiKeyResolver);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ApplicationAuthentication.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String apiKey = Optional.ofNullable(webRequest)
                .map(s -> s.getHeaderValues(HttpHeaders.AUTHORIZATION))
                .map(Arrays::stream).orElseGet(Stream::empty)
                .findFirst()
                .filter(this::isValidApiKey)
                .orElseThrow(() -> new BadRequestException("Authorization header not present or invalid"));

        Application application = Optional.of(apiKeyResolver.apply(apiKey))
                .map(Mono::block)
                .orElseThrow(() -> new NotFoundException("ApplicationEntity not found"));

        return ApplicationAuthenticationImpl.builder()
                .application(application)
                .build();
    }

    private boolean isValidApiKey(String key) {
        int keyLength = key.length();
        boolean hasValidLength = 8 <= keyLength && keyLength <= 1024;
        boolean hasValidChars = CharMatcher.JAVA_LETTER_OR_DIGIT.matchesAllOf(key);

        return hasValidLength && hasValidChars;
    }
}
