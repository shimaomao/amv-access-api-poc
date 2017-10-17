package org.amv.access.api.device;

import org.amv.access.AmvAccessApplication;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.CreateDeviceCertificateResponseDto;
import org.amv.access.client.model.DeviceCertificateDto;
import org.amv.access.config.TestDbConfig;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.model.ApplicationRepository;
import org.amv.access.util.SecureRandomUtils;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.amv.highmobility.cryptotool.CryptotoolWithIssuer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.apache.commons.codec.binary.Base64.isBase64;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AmvAccessApplication.class, TestDbConfig.class}
)
public class DeviceCertificateCtrlTest {

    @Autowired
    private CryptotoolWithIssuer cryptotool;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    private ApplicationEntity application;

    @Before
    public void setUp() {
        this.application = applicationRepository.save(ApplicationEntity.builder()
                .name("Test ApplicationEntity")
                .appId(SecureRandomUtils.generateRandomAppId())
                .apiKey(RandomStringUtils.randomAlphanumeric(8))
                .enabled(true)
                .build());
    }

    @Test
    public void itShouldFailCreatingDeviceCertificateIfAuthHeaderIsMissing() throws Exception {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();
        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        CreateDeviceCertificateRequestDto body = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey(publicKeyBase64)
                .build();

        ResponseEntity<?> responseEntity = restTemplate
                .postForEntity("/api/v1/device_certificates", body,
                        CreateDeviceCertificateResponseDto.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void itShouldFailCreatingDeviceCertificateIfApplicationDoesNotExist() throws Exception {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();
        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        CreateDeviceCertificateRequestDto body = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey(publicKeyBase64)
                .build();

        String nonExistingApiKey = application.getApiKey() + "123";

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, nonExistingApiKey);
        HttpEntity<CreateDeviceCertificateRequestDto> entity = new HttpEntity<>(body, headers);

        ResponseEntity<?> responseEntity = restTemplate
                .postForEntity("/api/v1/device_certificates", entity,
                        CreateDeviceCertificateResponseDto.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

    @Test
    public void itShouldCreateDeviceCertificateSuccessfully() throws Exception {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();
        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        CreateDeviceCertificateRequestDto body = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey(publicKeyBase64)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, application.getApiKey());
        HttpEntity<CreateDeviceCertificateRequestDto> entity = new HttpEntity<>(body, headers);

        ResponseEntity<CreateDeviceCertificateResponseDto> responseEntity = restTemplate
                .postForEntity("/api/v1/device_certificates", entity,
                        CreateDeviceCertificateResponseDto.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.CREATED));

        CreateDeviceCertificateResponseDto response = responseEntity.getBody();
        assertThat(response, is(notNullValue()));

        DeviceCertificateDto responseDeviceCertificate = response.getDeviceCertificate();
        assertThat(responseDeviceCertificate, is(notNullValue()));

        String deviceCertificate = responseDeviceCertificate.getDeviceCertificate();
        assertThat(deviceCertificate, is(notNullValue()));
        assertThat(isBase64(deviceCertificate), is(true));

        final String expectedIssuerPublicKey = cryptotool.getCertificateIssuer().getPublicKeyBase64();
        assertThat(responseDeviceCertificate.getIssuerPublicKey(), is(expectedIssuerPublicKey));
    }

    @Test
    @Ignore("not implemented yet")
    public void itShouldRevokeDeviceCertificate() throws Exception {
        // TODO: implement me
    }
}