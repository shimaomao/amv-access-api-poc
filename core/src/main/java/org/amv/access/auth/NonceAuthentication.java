package org.amv.access.auth;

public interface NonceAuthentication {
    String getNonceBase64();

    String getNonceSignatureBase64();
}
