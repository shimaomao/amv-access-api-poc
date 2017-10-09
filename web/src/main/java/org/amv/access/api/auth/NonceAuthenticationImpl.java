package org.amv.access.api.auth;

import lombok.Builder;
import lombok.Value;
import org.amv.access.model.Device;

@Value
@Builder
public class NonceAuthenticationImpl implements NonceAuthentication {

    private String nonce;
    private String signedNonce;

    @Override
    public boolean isValid(Device device) {
        return false;
    }
}
