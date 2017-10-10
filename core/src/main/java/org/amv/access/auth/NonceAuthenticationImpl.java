package org.amv.access.auth;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class NonceAuthenticationImpl implements NonceAuthentication {
    @NonNull
    private String nonce;
    @NonNull
    private String signedNonce;
}
