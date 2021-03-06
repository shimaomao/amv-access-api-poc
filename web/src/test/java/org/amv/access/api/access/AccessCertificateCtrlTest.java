package org.amv.access.api.access;

import org.amv.access.AmvAccessApplication;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.client.android.MoreHttpHeaders;
import org.amv.access.client.model.*;
import org.amv.access.client.model.CreateAccessCertificateResponseDto.AccessCertificateSigningRequestDto;
import org.amv.access.config.SqliteTestDatabaseConfig;
import org.amv.access.core.SignedAccessCertificate;
import org.amv.access.core.impl.AccessCertificateImpl;
import org.amv.access.demo.DemoService;
import org.amv.access.demo.DeviceWithKeys;
import org.amv.access.demo.IssuerWithKeys;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.UnauthorizedException;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.model.DeviceEntity;
import org.amv.access.model.VehicleEntity;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.amv.access.spi.highmobility.NonceAuthenticationService;
import org.amv.access.spi.highmobility.NonceAuthenticationServiceImpl;
import org.amv.access.spi.model.SignCertificateRequestImpl;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.amv.highmobility.cryptotool.CryptotoolUtils.decodeBase64AsHex;
import static org.apache.commons.codec.binary.Base64.isBase64;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AmvAccessApplication.class, SqliteTestDatabaseConfig.class}
)
public class AccessCertificateCtrlTest {
    @Autowired
    private Cryptotool cryptotool;

    @Autowired
    private AmvAccessModuleSpi accessModule;

    @Autowired
    private DemoService demoService;

    @Autowired
    private TestRestTemplate restTemplate;

    private ApplicationEntity application;

    private DeviceWithKeys deviceWithKeys;
    private IssuerWithKeys demoIssuer;
    private NonceAuthenticationService nonceAuthService;

    @Before
    public void setUp() {
        this.nonceAuthService = new NonceAuthenticationServiceImpl(cryptotool);
        this.application = demoService.getOrCreateDemoApplication();

        this.demoIssuer = demoService.getOrCreateDemoIssuer();

        this.deviceWithKeys = demoService.createDemoDeviceWithKeys(this.application);
    }

    @Test
    public void itShouldFailGetAccessCertificateIfNonceHeaderIsMissing() throws Exception {
        DeviceEntity device = deviceWithKeys.getDevice();

        HttpHeaders headers = new HttpHeaders();

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDto> responseEntity = restTemplate
                .exchange("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        HttpMethod.GET, entity, ErrorResponseDto.class,
                        device.getSerialNumber().toHex());

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.BAD_REQUEST));

        ErrorResponseDto errorResponseDto = responseEntity.getBody();
        assertThat(errorResponseDto.getErrors(), hasSize(1));

        ErrorResponseDto.ErrorInfoDto errorInfoDto = errorResponseDto.getErrors()
                .stream()
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        assertThat(errorInfoDto.getTitle(), is(BadRequestException.class.getSimpleName()));
        assertThat(errorInfoDto.getDetail(), is("amv-api-nonce header is missing"));
    }

    @Test
    public void itShouldFailGetAccessCertificateIfSignatureHeaderIsMissing() throws Exception {
        DeviceEntity device = deviceWithKeys.getDevice();

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, "42");

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDto> responseEntity = restTemplate
                .exchange("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        HttpMethod.GET, entity, ErrorResponseDto.class,
                        device.getSerialNumber().toHex());

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.BAD_REQUEST));

        ErrorResponseDto errorResponseDto = responseEntity.getBody();
        assertThat(errorResponseDto.getErrors(), hasSize(1));

        ErrorResponseDto.ErrorInfoDto errorInfoDto = errorResponseDto.getErrors()
                .stream()
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        assertThat(errorInfoDto.getTitle(), is(BadRequestException.class.getSimpleName()));
        assertThat(errorInfoDto.getDetail(), is("amv-api-signature header is missing"));
    }

    @Test
    public void itShouldFailGetAccessCertificateIfNonceSignatureHeaderIsInvalid() throws Exception {
        DeviceEntity device = deviceWithKeys.getDevice();

        String nonceBase64 = nonceAuthService
                .createNonceAuthentication(deviceWithKeys.getPrivateKey())
                .getNonceBase64();
        String mismatchingNonceSignatureBase64 = nonceAuthService
                .createNonceAuthentication(deviceWithKeys.getPrivateKey())
                .getNonceSignatureBase64();

        Boolean isSignedNonceValid = Optional.of(accessModule)
                .map(module -> module.verifySignature(nonceBase64, mismatchingNonceSignatureBase64, device.getPublicKey()))
                .map(Mono::block)
                .orElse(false);

        assertThat("Sanity check", isSignedNonceValid, is(false));

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, nonceBase64);
        headers.add(MoreHttpHeaders.AMV_SIGNATURE, mismatchingNonceSignatureBase64);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDto> responseEntity = restTemplate
                .exchange("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        HttpMethod.GET, entity, ErrorResponseDto.class,
                        device.getSerialNumber().toHex());

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.UNAUTHORIZED));

        ErrorResponseDto errorResponseDto = responseEntity.getBody();
        assertThat(errorResponseDto.getErrors(), hasSize(1));

        ErrorResponseDto.ErrorInfoDto errorInfoDto = errorResponseDto.getErrors()
                .stream()
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        assertThat(errorInfoDto.getTitle(), is(UnauthorizedException.class.getSimpleName()));
        assertThat(errorInfoDto.getDetail(), is("Signature is invalid"));
    }

    @Test
    public void itShouldGetEmptyAccessCertificateListSuccessfully() throws Exception {
        ResponseEntity<GetAccessCertificatesResponseDto> responseEntity =
                executeFetchAccessCertificatesRequest(deviceWithKeys);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertThat(responseEntity.getBody(), is(notNullValue()));

        GetAccessCertificatesResponseDto body = responseEntity.getBody();
        assertThat(body.getAccessCertificates(), is(empty()));
    }

    @Test
    public void itShouldCreateAccessCertificate() throws Exception {
        VehicleEntity vehicle = demoService.getOrCreateDemoVehicle();
        DeviceEntity device = deviceWithKeys.getDevice();

        CreateAccessCertificateRequestDto request = CreateAccessCertificateRequestDto.builder()
                .appId(application.getAppId().toHex())
                .deviceSerialNumber(device.getSerialNumber().toHex())
                .vehicleSerialNumber(vehicle.getSerialNumber().toHex())
                .build();

        ResponseEntity<CreateAccessCertificateResponseDto> createAccessCertificateResponse =
                executeCreateAccessCertificateRequest(demoIssuer, request);

        assertThat(createAccessCertificateResponse.getStatusCode(), is(HttpStatus.OK));

        CreateAccessCertificateResponseDto response = createAccessCertificateResponse.getBody();
        assertThat(response, is(notNullValue()));

        AccessCertificateSigningRequestDto signingRequest = response.getAccessCertificateSigningRequest();

        assertThat(signingRequest.getDeviceAccessCertificate(), is(notNullValue()));
        assertThat(isBase64(signingRequest.getDeviceAccessCertificate()), is(true));

        assertThat(signingRequest.getVehicleAccessCertificate(), is(notNullValue()));
        assertThat(isBase64(signingRequest.getVehicleAccessCertificate()), is(true));
    }

    @Test
    public void itShouldNotIncludeUnsignedAccessCertificatesInResponse() throws Exception {
        itShouldCreateAccessCertificate();

        List<AccessCertificateDto> accessCertificates = executeFetchAccessCertificatesRequest(deviceWithKeys)
                .getBody()
                .getAccessCertificates();

        assertThat(accessCertificates, hasSize(0));
    }

    @Test
    public void itShouldCreateAndThenSignAccessCertificate() throws Exception {
        VehicleEntity vehicle = demoService.getOrCreateDemoVehicle();
        DeviceEntity device = deviceWithKeys.getDevice();

        CreateAccessCertificateRequestDto postBody = CreateAccessCertificateRequestDto.builder()
                .appId(application.getAppId().toHex())
                .deviceSerialNumber(device.getSerialNumber().toHex())
                .vehicleSerialNumber(vehicle.getSerialNumber().toHex())
                .build();

        ResponseEntity<CreateAccessCertificateResponseDto> createAccessCertificateResponse =
                executeCreateAccessCertificateRequest(demoIssuer, postBody);

        assertThat(createAccessCertificateResponse.getStatusCode(), is(HttpStatus.OK));

        CreateAccessCertificateResponseDto postResponse = createAccessCertificateResponse.getBody();
        assertThat(postResponse, is(notNullValue()));

        AccessCertificateSigningRequestDto signingRequest = postResponse.getAccessCertificateSigningRequest();

        assertThat(signingRequest.getDeviceAccessCertificate(), is(notNullValue()));
        assertThat(isBase64(signingRequest.getDeviceAccessCertificate()), is(true));

        assertThat(signingRequest.getVehicleAccessCertificate(), is(notNullValue()));
        assertThat(isBase64(signingRequest.getVehicleAccessCertificate()), is(true));

        SignedAccessCertificate signedAccessCertificate = Optional.of(accessModule)
                .map(m -> m.signAccessCertificate(SignCertificateRequestImpl.builder()
                        .accessCertificate(AccessCertificateImpl.builder()
                                .deviceAccessCertificateBase64(signingRequest.getDeviceAccessCertificate())
                                .vehicleAccessCertificateBase64(signingRequest.getVehicleAccessCertificate())
                                .build())
                        .privateKey(demoIssuer.getPrivateKey())
                        .publicKey(demoIssuer.getPublicKey())
                        .build()))
                .map(Mono::block)
                .orElseThrow(IllegalStateException::new);

        UpdateAccessCertificateRequestDto putBody = UpdateAccessCertificateRequestDto.builder()
                .deviceAccessCertificateSignatureBase64(signedAccessCertificate.getDeviceAccessCertificateSignatureBase64())
                .signedDeviceAccessCertificateBase64(signedAccessCertificate.getSignedDeviceAccessCertificateBase64())
                .vehicleAccessCertificateSignatureBase64(signedAccessCertificate.getVehicleAccessCertificateSignatureBase64())
                .signedVehicleAccessCertificateBase64(signedAccessCertificate.getSignedVehicleAccessCertificateBase64())
                .build();

        ResponseEntity<Boolean> addAccessCertificateSignatureResponse = executeAddAccessCertificateSignaturesRequest(
                demoIssuer, signingRequest.getId(), putBody
        );

        assertThat(createAccessCertificateResponse.getStatusCode(), is(HttpStatus.OK));

        Boolean putResponse = addAccessCertificateSignatureResponse.getBody();
        assertThat(putResponse, is(notNullValue()));
        assertThat(putResponse, equalTo(true));
    }

    @Test
    public void itShouldCreateAndThenFetchAllAccessCertificates() throws Exception {
        itShouldCreateAndThenSignAccessCertificate();

        ResponseEntity<GetAccessCertificatesResponseDto> accessCertificateResponse =
                executeFetchAccessCertificatesRequest(deviceWithKeys);

        assertThat(accessCertificateResponse.getStatusCode(), is(HttpStatus.OK));

        GetAccessCertificatesResponseDto body = accessCertificateResponse.getBody();
        assertThat(body, is(notNullValue()));

        List<AccessCertificateDto> accessCertificates = body.getAccessCertificates();
        assertThat(accessCertificates, is(notNullValue()));
        assertThat(accessCertificates, hasSize(1));

        AccessCertificateDto accessCertificateDto = accessCertificates.get(0);
        assertThat(accessCertificateDto.getId(), is(notNullValue()));
        assertThat(accessCertificateDto.getName(), is(notNullValue()));
        assertThat(accessCertificateDto.getDeviceAccessCertificate(), is(notNullValue()));

        String signedDeviceAccessCertificateInHex = decodeBase64AsHex(accessCertificateDto.getDeviceAccessCertificate());

        String deviceAccessCertificateInHex = signedDeviceAccessCertificateInHex.substring(0, 200);
        String deviceAccessSignatureInHex = signedDeviceAccessCertificateInHex.substring(200,
                signedDeviceAccessCertificateInHex.length());

        String issuerPublicKeyInHex = decodeBase64AsHex(demoIssuer.getIssuer().getPublicKeyBase64());
        Cryptotool.Validity validity = Optional.of(cryptotool.verifySignature(deviceAccessCertificateInHex,
                deviceAccessSignatureInHex, issuerPublicKeyInHex))
                .map(Mono::block)
                .filter(val -> val == Cryptotool.Validity.VALID)
                .orElseThrow(IllegalStateException::new);

        assertThat(validity, is(Cryptotool.Validity.VALID));
    }


    @Test
    public void itShouldRevokeAccessCertificate() throws Exception {
        itShouldCreateAndThenFetchAllAccessCertificates();

        List<AccessCertificateDto> accessCertificatesBeforeRevocation = executeFetchAccessCertificatesRequest(deviceWithKeys)
                .getBody()
                .getAccessCertificates();

        AccessCertificateDto firstAccessCertificate = accessCertificatesBeforeRevocation
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("There should be at least one cert by now."));

        ResponseEntity<Boolean> deleteResponse = executeRevokeAccessCertificateRequest(demoIssuer,
                firstAccessCertificate.getId());

        assertThat(deleteResponse.getStatusCode(), is(HttpStatus.NO_CONTENT));

        List<AccessCertificateDto> accessCertificatesAfterRevocation = executeFetchAccessCertificatesRequest(deviceWithKeys)
                .getBody().getAccessCertificates();

        assertThat(accessCertificatesAfterRevocation.size() + 1, is(accessCertificatesBeforeRevocation.size()));

    }

    @Test
    public void itShouldThrowExceptionOnMismatchingIssuerWhenCreatingAccessCertificates() {
        // TODO: implement me
    }

    @Test
    public void itShouldThrowExceptionIfAccessCertificateCouldNotBeFoundWhenAddingSignatures() {
        // TODO: implement me
    }

    @Test
    public void itShouldReturnNoAccessCertificatesForDisabledDevice() {
        // TODO: implemnet me
    }

    private ResponseEntity<CreateAccessCertificateResponseDto> executeCreateAccessCertificateRequest(
            IssuerWithKeys issuerWithKeys, CreateAccessCertificateRequestDto request) {

        NonceAuthentication issuerNonceAuthentication = nonceAuthService
                .createAndVerifyNonceAuthentication(issuerWithKeys.getPrivateKey(), issuerWithKeys.getPublicKey());

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, issuerNonceAuthentication.getNonceBase64());
        headers.add(MoreHttpHeaders.AMV_SIGNATURE, issuerNonceAuthentication.getNonceSignatureBase64());

        HttpEntity<CreateAccessCertificateRequestDto> postEntity = new HttpEntity<>(request, headers);

        ResponseEntity<CreateAccessCertificateResponseDto> createAccessCertificateResponse = restTemplate
                .postForEntity("/api/v1/issuer/{issuerUuid}/access_certificates",
                        postEntity,
                        CreateAccessCertificateResponseDto.class,
                        issuerWithKeys.getIssuer().getUuid());

        return createAccessCertificateResponse;
    }

    private ResponseEntity<Boolean> executeAddAccessCertificateSignaturesRequest(
            IssuerWithKeys issuerWithKeys, String accessCertificateId,
            UpdateAccessCertificateRequestDto request) {

        NonceAuthentication issuerNonceAuthentication = nonceAuthService
                .createAndVerifyNonceAuthentication(issuerWithKeys.getPrivateKey(), issuerWithKeys.getPublicKey());

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, issuerNonceAuthentication.getNonceBase64());
        headers.add(MoreHttpHeaders.AMV_SIGNATURE, issuerNonceAuthentication.getNonceSignatureBase64());

        HttpEntity<UpdateAccessCertificateRequestDto> putEntity = new HttpEntity<>(request, headers);

        ResponseEntity<Boolean> addAccessCertificateSignatureResponse = restTemplate
                .exchange("/api/v1/issuer/{issuerUuid}/access_certificates/{accessCertificateId}/signature",
                        HttpMethod.PUT,
                        putEntity,
                        Boolean.class,
                        issuerWithKeys.getIssuer().getUuid(),
                        accessCertificateId);

        return addAccessCertificateSignatureResponse;
    }


    private ResponseEntity<GetAccessCertificatesResponseDto> executeFetchAccessCertificatesRequest(
            DeviceWithKeys deviceWithKeys) {
        NonceAuthentication deviceNonceAuthentication = nonceAuthService
                .createAndVerifyNonceAuthentication(deviceWithKeys.getPrivateKey(), deviceWithKeys.getPublicKey());

        HttpHeaders fetchRequestHeaders = new HttpHeaders();
        fetchRequestHeaders.add(MoreHttpHeaders.AMV_NONCE, deviceNonceAuthentication.getNonceBase64());
        fetchRequestHeaders.add(MoreHttpHeaders.AMV_SIGNATURE, deviceNonceAuthentication.getNonceSignatureBase64());

        HttpEntity<CreateDeviceCertificateRequestDto> fetchEntity = new HttpEntity<>(fetchRequestHeaders);

        ResponseEntity<GetAccessCertificatesResponseDto> getAccessCertificateResponse = restTemplate
                .exchange("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        HttpMethod.GET, fetchEntity, GetAccessCertificatesResponseDto.class,
                        deviceWithKeys.getDevice().getSerialNumber().toHex());

        return getAccessCertificateResponse;
    }

    private ResponseEntity<Boolean> executeRevokeAccessCertificateRequest(IssuerWithKeys issuerWithKeys,
                                                                          String accessCertificateId) {
        NonceAuthentication issuerNonceAuthentication = nonceAuthService
                .createAndVerifyNonceAuthentication(issuerWithKeys.getPrivateKey(), issuerWithKeys.getPublicKey());

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, issuerNonceAuthentication.getNonceBase64());
        headers.add(MoreHttpHeaders.AMV_SIGNATURE, issuerNonceAuthentication.getNonceSignatureBase64());

        HttpEntity<CreateAccessCertificateRequestDto> postEntity = new HttpEntity<>(headers);

        ResponseEntity<Boolean> deleteResponse = restTemplate
                .exchange("/api/v1/issuer/{issuerUuid}/access_certificates/{accessCertificateId}",
                        HttpMethod.DELETE,
                        postEntity,
                        Boolean.class,
                        issuerWithKeys.getIssuer().getUuid(),
                        accessCertificateId);

        return deleteResponse;
    }
}
