package org.amv.access.auth;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class NonceAuthenticationImpl implements NonceAuthentication {
    @NonNull
    private String nonceBase64;
    @NonNull
    private String nonceSignatureBase64;
}
