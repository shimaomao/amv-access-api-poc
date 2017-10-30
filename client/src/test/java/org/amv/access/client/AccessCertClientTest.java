package org.amv.access.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;
import com.netflix.hystrix.HystrixCommand;
import feign.Response;
import feign.Target;
import feign.mock.HttpMethod;
import feign.mock.MockClient;
import feign.mock.MockTarget;
import org.amv.access.client.model.GetAccessCertificatesResponseDto;
import org.amv.access.client.model.GetAccessCertificatesResponseDtoObjectMother;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class AccessCertClientTest {

    private static final String RANDOM_DEVICE_SERIAL = RandomStringUtils.randomAlphanumeric(10);

    @Test
    public void itShouldFetchAccessCertificatesSuccessfully() throws JsonProcessingException {
        GetAccessCertificatesResponseDto accessCertificatesResponseDto = GetAccessCertificatesResponseDtoObjectMother.random();
        String accessCertificatesResponseDtoAsJson = Clients.defaultObjectMapper.writeValueAsString(accessCertificatesResponseDto);

        MockClient mockClient = new MockClient()
                .add(HttpMethod.GET, String.format("/api/v1/device/%s/access_certificates", RANDOM_DEVICE_SERIAL), Response.builder()
                        .status(HttpStatus.OK.value())
                        .reason(HttpStatus.OK.getReasonPhrase())
                        .headers(Collections.<String, Collection<String>>emptyMap())
                        .body(accessCertificatesResponseDtoAsJson, Charsets.UTF_8));

        Target<AccessCertClient> mockTarget = new MockTarget<AccessCertClient>(AccessCertClient.class);

        AccessCertClient sut = Clients.simpleFeignBuilder()
                .client(mockClient)
                .build()
                .newInstance(mockTarget);

        HystrixCommand<GetAccessCertificatesResponseDto> accessCertificatesRequest = sut
                .fetchAccessCertificates("", "", RANDOM_DEVICE_SERIAL);

        GetAccessCertificatesResponseDto accessCertificatesResponse = accessCertificatesRequest.execute();

        assertThat(accessCertificatesResponse, is(notNullValue()));
        assertThat(accessCertificatesResponse, is(accessCertificatesResponseDto));
        assertThat(accessCertificatesResponse.getAccessCertificates(), hasSize(accessCertificatesResponseDto.getAccessCertificates().size()));
    }
}
