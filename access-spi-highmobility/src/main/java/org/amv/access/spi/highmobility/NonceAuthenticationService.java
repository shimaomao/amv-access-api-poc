package org.amv.access.spi.highmobility;

import org.amv.access.auth.NonceAuthentication;
import org.amv.access.core.Key;

public interface NonceAuthenticationService {
    NonceAuthentication createNonceAuthentication(Key privateKey);

    NonceAuthentication createAndVerifyNonceAuthentication(Key privateKey, Key publicKey);
}
