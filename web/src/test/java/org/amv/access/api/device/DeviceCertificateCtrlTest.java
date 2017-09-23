package org.amv.access.api.device;

import org.amv.access.Application;
import org.amv.access.api.device.model.CreateDeviceCertificateRequest;
import org.amv.access.api.device.model.CreateDeviceCertificateResponse;
import org.amv.access.api.device.model.DeviceCertificateDto;
import org.amv.access.config.TestDbConfig;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.amv.highmobility.cryptotool.CryptotoolUtils.TestUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.apache.commons.codec.binary.Base64.isBase64;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {Application.class, TestDbConfig.class}
)
public class DeviceCertificateCtrlTest {

    @Autowired
    private Cryptotool cryptotool;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void itShouldCreateDeviceCertificate() throws Exception {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();
        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        CreateDeviceCertificateRequest request = CreateDeviceCertificateRequest.builder()
                .appId(TestUtils.generateRandomAppId())
                .publicKey(publicKeyBase64)
                .build();

        ResponseEntity<CreateDeviceCertificateResponse> responseEntity = restTemplate
                .postForEntity("/device_certificates", request,
                        CreateDeviceCertificateResponse.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));

        CreateDeviceCertificateResponse response = responseEntity.getBody();
        assertThat(response, is(notNullValue()));

        DeviceCertificateDto deviceCertificate = response.getDeviceCertificate();

        assertThat(deviceCertificate.getAppId(), is(equalTo(request.getAppId())));
        assertThat(deviceCertificate.getDeviceName(), is(equalTo(request.getName())));

        assertThat(deviceCertificate.getCertificate(), is(notNullValue()));
        assertThat(isBase64(deviceCertificate.getCertificate()), is(true));
    }

    @Test
    @Ignore("not implemented yet")
    public void itShouldRevokeDeviceCertificate() throws Exception {
        // TODO: implement me
    }
}