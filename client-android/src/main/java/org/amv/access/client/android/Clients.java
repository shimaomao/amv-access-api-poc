package org.amv.access.client.android;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.MediaType;

public final class Clients {
    static final MediaType JSON = MediaType.parse("application/json;charset=utf-8");

    static final Gson defaultObjectMapper = new GsonBuilder().create();

    private Clients() {
        throw new UnsupportedOperationException();
    }


    public static DeviceCertClient simpleDeviceCertClient(String baseUrl) {
        if (baseUrl == null) {
            throw new IllegalArgumentException("`baseUrl` must not be null");
        }

        return new SimpleDeviceCertClient(baseUrl);
    }

    public static AccessCertClient simpleAccessCertClient(String baseUrl) {
        if (baseUrl == null) {
            throw new IllegalArgumentException("`baseUrl` must not be null");
        }

        return new SimpleAccessCertClient(baseUrl);
    }
}
