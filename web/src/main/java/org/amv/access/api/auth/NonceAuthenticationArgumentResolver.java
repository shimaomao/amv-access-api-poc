package org.amv.access.api.auth;

import com.google.common.collect.ImmutableList;
import org.amv.access.api.MoreHttpHeaders;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

public class NonceAuthenticationArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return NonceAuthentication.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String nonce = getNonceOrThrow(webRequest);
        String signedNonce = getNonceOrThrow(webRequest);

        return NonceAuthenticationImpl.builder()
                .nonce(nonce)
                .signedNonce(signedNonce)
                .build();
    }

    private String getNonceOrThrow(NativeWebRequest webRequest) {
        return Optional.ofNullable(webRequest)
                .map(s -> s.getHeaderValues(MoreHttpHeaders.AMV_NONCE))
                .map(ImmutableList::copyOf)
                .orElseGet(ImmutableList::of)
                .stream()
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nonce header not present"));
    }

    private String getSignedNonceOrThrow(NativeWebRequest webRequest) {
        return Optional.ofNullable(webRequest)
                .map(s -> s.getHeaderValues(MoreHttpHeaders.AMV_NONCE))
                .map(ImmutableList::copyOf)
                .orElseGet(ImmutableList::of)
                .stream()
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nonce header not present"));
    }
}
