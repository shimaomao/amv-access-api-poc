package org.amv.access.api.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.amv.access.api.device.model.CreateDeviceCertificateResponse;
import org.amv.access.api.device.model.DeviceCertificateDto;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class CreateDeviceCertificateResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true);

    @Test
    public void itShouldEmbedDeviceCertificateInJsonFormat() throws IOException {
        DeviceCertificateDto deviceCertificateDto = DeviceCertificateDto.builder()
                .id(RandomUtils.nextLong())
                .appId(RandomStringUtils.randomAlphanumeric(10))
                .certificate(RandomStringUtils.randomAlphanumeric(10))
                .issuerName(RandomStringUtils.randomAlphanumeric(10))
                .issuerPublicKey(RandomStringUtils.randomAlphanumeric(10))
                .deviceSerialNumber(RandomStringUtils.randomAlphanumeric(10))
                .deviceName(RandomStringUtils.randomAlphanumeric(10))
                .build();

        CreateDeviceCertificateResponse response = CreateDeviceCertificateResponse.builder()
                .deviceCertificate(deviceCertificateDto)
                .build();

        String json = objectMapper.writeValueAsString(response);
        JsonNode jsonNode = objectMapper.readTree(json);

        assertThat(jsonNode.get("id").asLong(), is(equalTo(deviceCertificateDto.getId())));
        assertThat(jsonNode.get("app_id").asText(), is(equalTo(deviceCertificateDto.getAppId())));
        assertThat(jsonNode.get("certificate").asText(), is(equalTo(deviceCertificateDto.getCertificate())));
        assertThat(jsonNode.get("issuer_name").asText(), is(equalTo(deviceCertificateDto.getIssuerName())));
        assertThat(jsonNode.get("issuer_public_key").asText(), is(equalTo(deviceCertificateDto.getIssuerPublicKey())));
        assertThat(jsonNode.get("device_serial_number").asText(), is(equalTo(deviceCertificateDto.getDeviceSerialNumber())));
        assertThat(jsonNode.get("device_name").asText(), is(equalTo(deviceCertificateDto.getDeviceName())));
    }
}