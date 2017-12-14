package org.amv.access.auth;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class DeviceNonceAuthenticationImpl implements DeviceNonceAuthentication {
    @NonNull
    private NonceAuthentication nonceAuthentication;
    @NonNull
    private String deviceSerialNumber;

    @Override
    public String getNonceBase64() {
        return nonceAuthentication.getNonceBase64();
    }

    @Override
    public String getNonceSignatureBase64() {
        return nonceAuthentication.getNonceSignatureBase64();
    }
}
