package org.amv.access.client.java6;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import okhttp3.MediaType;

import static com.google.common.base.Preconditions.checkArgument;

public final class Clients {
    static final MediaType JSON = MediaType.parse("application/json;charset=utf-8");

    static final ObjectMapper defaultObjectMapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
