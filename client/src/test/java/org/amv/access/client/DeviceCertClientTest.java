package org.amv.access.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;
import com.netflix.hystrix.HystrixCommand;
import feign.Response;
import feign.Target;
import feign.mock.HttpMethod;
import feign.mock.MockClient;
import feign.mock.MockTarget;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.CreateDeviceCertificateResponseDto;
import org.amv.access.client.model.CreateDeviceCertificateResponseDtoObjectMother;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DeviceCertClientTest {

    private static final String RANDOM_API_KEY = RandomStringUtils.randomAlphanumeric(10);

    @Test
    public void createDeviceCertificate() throws JsonProcessingException {
        CreateDeviceCertificateResponseDto deviceCertificate = CreateDeviceCertificateResponseDtoObjectMother.random();
        String deviceCertificateAsJson = Clients.defaultObjectMapper.writeValueAsString(deviceCertificate);

        MockClient mockClient = new MockClient()
                .add(HttpMethod.POST, "/api/v1/device_certificates", Response.builder()
                        .status(HttpStatus.CREATED.value())
                        .reason(HttpStatus.CREATED.getReasonPhrase())
                        .headers(Collections.<String, Collection<String>>emptyMap())
                        .body(deviceCertificateAsJson, Charsets.UTF_8));

        Target<DeviceCertClient> mockTarget = new MockTarget<DeviceCertClient>(DeviceCertClient.class);

        DeviceCertClient sut = Clients.simpleFeignBuilder()
                .client(mockClient)
                .build()
                .newInstance(mockTarget);

        CreateDeviceCertificateRequestDto request = CreateDeviceCertificateRequestDto.builder()
                .build();

        HystrixCommand<CreateDeviceCertificateResponseDto> deviceCertificateRequest = sut
                .createDeviceCertificate(RANDOM_API_KEY, request);

        CreateDeviceCertificateResponseDto deviceCertificateResponse = deviceCertificateRequest.execute();

        assertThat(deviceCertificateResponse, is(notNullValue()));
        assertThat(deviceCertificateResponse.getDeviceCertificate(), is(deviceCertificate.getDeviceCertificate()));
    }
}
