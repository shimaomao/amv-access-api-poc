package org.amv.access.client.java6;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.MediaType;

import static com.google.common.base.Preconditions.checkArgument;

public final class Clients {
    static final MediaType JSON = MediaType.parse("application/json;charset=utf-8");

    static final Gson defaultObjectMapper = new GsonBuilder().create();

    private Clients() {
        throw new UnsupportedOperationException();
    }


    public static DeviceCertClient simpleDeviceCertClient(String baseUrl) {
        checkArgument(baseUrl != null);

        return new SimpleDeviceCertClient(baseUrl);
    }

    public static AccessCertClient simpleAccessCertClient(String baseUrl) {
        checkArgument(baseUrl != null);

        return new SimpleAccessCertClient(baseUrl);
    }
}
