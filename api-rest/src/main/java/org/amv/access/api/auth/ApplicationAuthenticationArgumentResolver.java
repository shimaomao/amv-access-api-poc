package org.amv.access.api.auth;

import com.google.common.base.CharMatcher;
import com.google.common.net.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.ApplicationAuthentication;
import org.amv.access.auth.ApplicationAuthenticationImpl;
import org.amv.access.core.Application;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@Slf4j
public class ApplicationAuthenticationArgumentResolver implements HandlerMethodArgumentResolver {

    @FunctionalInterface
    public interface ApplicationResolver {
        Mono<Application> find(String appId, String apiKey);
    }

    private static final String HEADER_NAME = HttpHeaders.AUTHORIZATION;

    private static final CharMatcher APP_ID_MATCHER = CharMatcher.inRange('a', 'z')
            .or(CharMatcher.inRange('A', 'Z'))
            .or(CharMatcher.inRange('0', '9'))
            .precomputed();

    private static final CharMatcher API_KEY_MATCHER = CharMatcher.anyOf("-")
            .or(CharMatcher.inRange('a', 'z'))
            .or(CharMatcher.inRange('A', 'Z'))
            .or(CharMatcher.inRange('0', '9'))
            .precomputed();

    private final ApplicationResolver applicationResolver;

    public ApplicationAuthenticationArgumentResolver(ApplicationResolver applicationResolver) {
        this.applicationResolver = requireNonNull(applicationResolver);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ApplicationAuthentication.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        Optional<String> authorizationHeaderValueOrEmpty = Optional.ofNullable(webRequest)
                .map(s -> s.getHeaderValues(HEADER_NAME))
                .map(Arrays::stream).orElseGet(Stream::empty)
                .findFirst();

        if (!authorizationHeaderValueOrEmpty.isPresent()) {
            throw new BadRequestException(HEADER_NAME + " header is invalid or missing");
        }

        String authorizationHeaderValue = authorizationHeaderValueOrEmpty.get();

        String[] authorizationHeaderValueParts = authorizationHeaderValue.split(":", 2);

        if (authorizationHeaderValueParts.length != 2) {
            throw new BadRequestException(HEADER_NAME + " header is invalid or missing");
        }

        String appId = Optional.ofNullable(authorizationHeaderValueParts[0])
                .filter(this::isValidAppId)
                .orElseThrow(() -> new BadRequestException(HEADER_NAME + " header is invalid or missing"));

        String apiKey = Optional.ofNullable(authorizationHeaderValueParts[1])
                .filter(this::isValidApiKey)
                .orElseThrow(() -> new BadRequestException(HEADER_NAME + " header is invalid or missing"));

        Application application = Optional.of(applicationResolver.find(appId, apiKey))
                .map(app -> app.onErrorMap(e -> new UnauthorizedException(e.getMessage(), e)))
                .map(Mono::block)
                .orElseThrow(() -> new UnauthorizedException("ApplicationEntity not found"));

        return ApplicationAuthenticationImpl.builder()
                .application(application)
                .build();
    }

    private boolean isValidAppId(String appId) {
        int appIdLength = appId.length();
        boolean hasValidLength = appIdLength == 24;
        boolean hasValidChars = APP_ID_MATCHER.matchesAllOf(appId);

        return hasValidLength && hasValidChars;
    }

    private boolean isValidApiKey(String key) {
        int keyLength = key.length();
        boolean hasValidLength = 8 <= keyLength && keyLength <= 255;
        boolean hasValidChars = API_KEY_MATCHER.matchesAllOf(key);

        return hasValidLength && hasValidChars;
    }
}
