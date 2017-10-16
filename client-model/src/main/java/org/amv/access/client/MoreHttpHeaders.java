package org.amv.access.client;

public final class MoreHttpHeaders {

    private MoreHttpHeaders() {
        throw new UnsupportedOperationException();
    }

    public static final String X_REASON = "X-Reason";
    public static final String AMV_NONCE = "amv-api-nonce";
    public static final String AMV_SIGNATURE = "amv-api-signature";
}
