package org.amv.access.client.android;

public final class MoreHttpHeaders {
    public static final String X_REASON = "X-Reason";
    public static final String AMV_NONCE = "amv-api-nonce";
    public static final String AMV_SIGNATURE = "amv-api-signature";

    private MoreHttpHeaders() {
        throw new UnsupportedOperationException();
    }
}
