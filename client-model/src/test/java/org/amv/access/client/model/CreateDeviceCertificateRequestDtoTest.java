package org.amv.access.client.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CreateDeviceCertificateRequestDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true);

    @Test
    public void itShouldBeDeserializableFromJsonFormat() throws IOException {
        String exampleJsonFromOnlineDoc = "{" +
                "\"device_public_key\": \"6KGWf1MAqTsrF7VinQjK6mEViomZ94vzvsCdXSOPvPd3oHZyFVW80qWlktq1uwthIHRrOmA/kEY5QJ1BruST2w==\"" +
                "}";

        JsonNode exampleJsonNode = objectMapper.readTree(exampleJsonFromOnlineDoc);

        CreateDeviceCertificateRequestDto request = objectMapper.readValue(exampleJsonFromOnlineDoc, CreateDeviceCertificateRequestDto.class);

        assertThat(request, is(notNullValue()));
        assertThat(request.getDevicePublicKey(), is(exampleJsonNode.get("device_public_key").asText()));
    }
}
