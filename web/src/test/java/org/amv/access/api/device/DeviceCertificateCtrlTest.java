package org.amv.access.api.device;

import org.amv.access.AmvAccessApplication;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.CreateDeviceCertificateResponseDto;
import org.amv.access.client.model.DeviceCertificateDto;
import org.amv.access.client.model.ErrorResponseDto;
import org.amv.access.config.SqliteTestDatabaseConfig;
import org.amv.access.core.Issuer;
import org.amv.access.demo.DemoService;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.UnauthorizedException;
import org.amv.access.model.ApplicationEntity;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
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
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.apache.commons.codec.binary.Base64.isBase64;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AmvAccessApplication.class, SqliteTestDatabaseConfig.class}
)
public class DeviceCertificateCtrlTest {

    @Autowired
    private Cryptotool cryptotool;

    @Autowired
    private DemoService demoService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Issuer issuer;

    private ApplicationEntity application;

    @Before
    public void setUp() {
        this.application = demoService.getOrCreateDemoApplication();
    }

    @Test
    public void itShouldFailCreatingDeviceCertificateIfAuthHeaderIsMissing() throws Exception {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();
        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        CreateDeviceCertificateRequestDto body = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey(publicKeyBase64)
                .build();

        ResponseEntity<ErrorResponseDto> responseEntity = restTemplate
                .postForEntity("/api/v1/device_certificates", body,
                        ErrorResponseDto.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.BAD_REQUEST));

        ErrorResponseDto errorResponseDto = responseEntity.getBody();
        assertThat(errorResponseDto.getErrors(), hasSize(1));

        ErrorResponseDto.ErrorInfoDto errorInfoDto = errorResponseDto.getErrors()
                .stream()
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        assertThat(errorInfoDto.getTitle(), is(BadRequestException.class.getSimpleName()));
        assertThat(errorInfoDto.getDetail(), is("Authorization header is invalid or missing"));
    }

    @Test
    public void itShouldFailCreatingDeviceCertificateIfApiKeyIsInvalid() throws Exception {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();
        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        CreateDeviceCertificateRequestDto body = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey(publicKeyBase64)
                .build();

        String keyWithIllegalChars = "@@_KEY_WITH_ILLEGAL_CHARACTERS_@@";

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, String.format("%s:%s", application.getAppId(), keyWithIllegalChars));
        HttpEntity<CreateDeviceCertificateRequestDto> entity = new HttpEntity<>(body, headers);

        ResponseEntity<ErrorResponseDto> responseEntity = restTemplate
                .postForEntity("/api/v1/device_certificates", entity,
                        ErrorResponseDto.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.BAD_REQUEST));

        ErrorResponseDto errorResponseDto = responseEntity.getBody();
        assertThat(errorResponseDto.getErrors(), hasSize(1));

        ErrorResponseDto.ErrorInfoDto errorInfoDto = errorResponseDto.getErrors()
                .stream()
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        assertThat(errorInfoDto.getTitle(), is(BadRequestException.class.getSimpleName()));
        assertThat(errorInfoDto.getDetail(), is("Authorization header is invalid or missing"));
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
        headers.add(HttpHeaders.AUTHORIZATION, String.format("%s:%s", application.getAppId(), nonExistingApiKey));
        HttpEntity<CreateDeviceCertificateRequestDto> entity = new HttpEntity<>(body, headers);

        ResponseEntity<ErrorResponseDto> responseEntity = restTemplate
                .postForEntity("/api/v1/device_certificates", entity,
                        ErrorResponseDto.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.UNAUTHORIZED));

        ErrorResponseDto errorResponseDto = responseEntity.getBody();
        assertThat(errorResponseDto.getErrors(), hasSize(1));

        ErrorResponseDto.ErrorInfoDto errorInfoDto = errorResponseDto.getErrors()
                .stream()
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        assertThat(errorInfoDto.getTitle(), is(UnauthorizedException.class.getSimpleName()));
        assertThat(errorInfoDto.getDetail(), is("ApplicationEntity not found"));
    }

    @Test
    public void itShouldCreateDeviceCertificateSuccessfully() throws Exception {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();
        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        CreateDeviceCertificateRequestDto body = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey(publicKeyBase64)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, String.format("%s:%s", application.getAppId(), application.getApiKey()));
        HttpEntity<CreateDeviceCertificateRequestDto> entity = new HttpEntity<>(body, headers);

        ResponseEntity<CreateDeviceCertificateResponseDto> responseEntity = restTemplate
                .postForEntity("/api/v1/device_certificates", entity,
                        CreateDeviceCertificateResponseDto.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.CREATED));

        CreateDeviceCertificateResponseDto response = responseEntity.getBody();
        assertThat(response, is(notNullValue()));

        DeviceCertificateDto responseDeviceCertificate = response.getDeviceCertificate();
        assertThat(responseDeviceCertificate, is(notNullValue()));

        String expectedIssuerPublicKey = issuer.getPublicKeyBase64();
        assertThat(responseDeviceCertificate.getIssuerPublicKey(), is(expectedIssuerPublicKey));

        String signedDeviceCertificate = responseDeviceCertificate.getDeviceCertificate();
        assertThat(signedDeviceCertificate, is(notNullValue()));
        assertThat(isBase64(signedDeviceCertificate), is(true));

        String signedDeviceCertificateInHex = CryptotoolUtils.decodeBase64AsHex(signedDeviceCertificate);

        String deviceCertificate = signedDeviceCertificateInHex.substring(0, 178);
        String signatureInHex = signedDeviceCertificateInHex.substring(178, signedDeviceCertificateInHex.length());

        String issuerPublicKeyInHex = CryptotoolUtils.decodeBase64AsHex(issuer.getPublicKeyBase64());
        Cryptotool.Validity validity = Optional.of(cryptotool.verifySignature(deviceCertificate, signatureInHex, issuerPublicKeyInHex))
                .map(Mono::block)
                .filter(val -> val == Cryptotool.Validity.VALID)
                .orElseThrow(IllegalStateException::new);

        assertThat(validity, is(Cryptotool.Validity.VALID));
    }

    @Test
    @Ignore("not implemented yet")
    public void itShouldRevokeDeviceCertificate() throws Exception {
        // TODO: implement me
    }
}
