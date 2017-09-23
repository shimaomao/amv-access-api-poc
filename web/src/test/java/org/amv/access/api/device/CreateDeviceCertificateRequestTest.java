package org.amv.access.api.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.amv.access.api.device.model.CreateDeviceCertificateRequest;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;


public class CreateDeviceCertificateRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true);

    @Test
    public void itShouldBeDeserializableFromJsonFormat() throws IOException {
        String exampleJsonFromOnlineDoc = "{" +
                "\"public_key\": \"6KGWf1MAqTsrF7VinQjK6mEViomZ94vzvsCdXSOPvPd3oHZyFVW80qWlktq1uwthIHRrOmA/kEY5QJ1BruST2w==\", " +
                "\"app_id\": \"A27A22A1B0A07E91B7CE7001\", " +
                "\"name\": \"Test Device\"" +
                "}";

        JsonNode exampleJsonNode = objectMapper.readTree(exampleJsonFromOnlineDoc);

        CreateDeviceCertificateRequest request = objectMapper.readValue(exampleJsonFromOnlineDoc, CreateDeviceCertificateRequest.class);

        assertThat(request, is(notNullValue()));
        assertThat(request.getPublicKey(), is(equalTo(exampleJsonNode.get("public_key").asText())));
        assertThat(request.getAppId(), is(equalTo(exampleJsonNode.get("app_id").asText())));
        assertThat(request.getName(), is(equalTo(exampleJsonNode.get("name").asText())));
    }
}