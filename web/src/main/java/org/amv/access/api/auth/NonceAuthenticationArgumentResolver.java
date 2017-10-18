package org.amv.access.api.auth;

import com.google.common.collect.ImmutableList;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.auth.NonceAuthenticationImpl;
import org.amv.access.client.MoreHttpHeaders;
import org.amv.access.exception.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Optional;

import static org.apache.commons.codec.binary.Base64.isBase64;

public class NonceAuthenticationArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return NonceAuthentication.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String nonceBase64 = getNonceOrThrow(webRequest);
        String nonceSignatureBase64 = getNonceSignatureOrThrow(webRequest);

        if (!isBase64(nonceBase64)) {
            throw new BadRequestException("Nonce must be base64");
        }
        if (!isBase64(nonceSignatureBase64)) {
            throw new BadRequestException("Nonce signature must be base64");
        }

        return NonceAuthenticationImpl.builder()
                .nonceBase64(nonceBase64)
                .nonceSignatureBase64(nonceSignatureBase64)
                .build();
    }

    private String getNonceOrThrow(NativeWebRequest webRequest) {
        return findFirstHeaderValueOrThrow(webRequest, MoreHttpHeaders.AMV_NONCE);
    }

    private String getNonceSignatureOrThrow(NativeWebRequest webRequest) {
        return findFirstHeaderValueOrThrow(webRequest, MoreHttpHeaders.AMV_SIGNATURE);
    }

    private String findFirstHeaderValueOrThrow(NativeWebRequest webRequest, String headerName) {
        return findHeaderValues(webRequest, headerName)
                .stream()
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(headerName + " header not present"));
    }

    private List<String> findHeaderValues(NativeWebRequest webRequest, String headerName) {
        return Optional.ofNullable(webRequest)
                .map(s -> s.getHeaderValues(headerName))
                .map(ImmutableList::copyOf)
                .orElseGet(ImmutableList::of);
    }
}
