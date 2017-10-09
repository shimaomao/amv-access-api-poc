package org.amv.access.auth;

public interface NonceAuthentication {
    String getNonce();

    String getSignedNonce();
}
