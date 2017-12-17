package org.amv.access.spi.highmobility;

import org.amv.access.auth.NonceAuthentication;
import org.amv.highmobility.cryptotool.Cryptotool;

public interface NonceAuthenticationService {
    NonceAuthentication createNonceAuthentication(Cryptotool.Keys keys);
}
