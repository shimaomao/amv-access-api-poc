package org.amv.access.api.auth;

import org.amv.access.model.Device;

public interface NonceAuthentication {
    String getNonce();

    String getSignedNonce();

    boolean isValid(Device device);
}
