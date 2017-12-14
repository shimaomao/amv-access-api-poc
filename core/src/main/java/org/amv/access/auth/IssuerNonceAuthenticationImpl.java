package org.amv.access.auth;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class IssuerNonceAuthenticationImpl implements IssuerNonceAuthentication {
    @NonNull
    private NonceAuthentication nonceAuthentication;
    @NonNull
    private String issuerUuid;

    @Override
    public String getNonceBase64() {
        return nonceAuthentication.getNonceBase64();
    }

    @Override
    public String getNonceSignatureBase64() {
        return nonceAuthentication.getNonceSignatureBase64();
    }
}
