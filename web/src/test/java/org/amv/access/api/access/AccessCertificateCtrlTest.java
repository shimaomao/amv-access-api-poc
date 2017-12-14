package org.amv.access.api.access;

import org.amv.access.AmvAccessApplication;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.client.MoreHttpHeaders;
import org.amv.access.client.model.*;
import org.amv.access.client.model.CreateAccessCertificateResponseDto.AccessCertificateSigningRequestDto;
import org.amv.access.config.SqliteTestDatabaseConfig;
import org.amv.access.demo.DemoService;
import org.amv.access.demo.DeviceWithKeys;
import org.amv.access.demo.IssuerWithKeys;
import org.amv.access.demo.NonceAuthHelper;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.UnauthorizedException;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.model.DeviceEntity;
import org.amv.access.model.VehicleEntity;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.junit.Before;
import org.junit.Ignore;
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
    private DemoService demoService;

    @Autowired
    private TestRestTemplate restTemplate;

    private ApplicationEntity application;

    private DeviceWithKeys deviceWithKeys;
    private IssuerWithKeys demoIssuer;
    private NonceAuthHelper nonceAuthHelper;

    @Before
    public void setUp() {
        this.nonceAuthHelper = new NonceAuthHelper(cryptotool);
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
                        device.getSerialNumber());

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
                        device.getSerialNumber());

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

        String nonceBase64 = nonceAuthHelper.createNonceWithRandomLengthBase64();
        String nonceBase64ForFakeSignature = nonceAuthHelper.createNonceWithRandomLengthBase64();
        String nonceSignatureBase64 = nonceAuthHelper.createNonceSignatureBase64(deviceWithKeys.getKeys(), nonceBase64ForFakeSignature);

        String devicePublicKey = decodeBase64AsHex(device.getPublicKeyBase64());
        Cryptotool.Validity signedNonceValidity = Optional.of(cryptotool.verifySignature(
                decodeBase64AsHex(nonceBase64),
                decodeBase64AsHex(nonceSignatureBase64),
                devicePublicKey))
                .map(Mono::block)
                .orElse(Cryptotool.Validity.VALID);

        assertThat("Sanity check", signedNonceValidity, is(Cryptotool.Validity.INVALID));

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, nonceBase64);
        headers.add(MoreHttpHeaders.AMV_SIGNATURE, nonceSignatureBase64);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDto> responseEntity = restTemplate
                .exchange("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        HttpMethod.GET, entity, ErrorResponseDto.class,
                        device.getSerialNumber());

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
        DeviceEntity device = deviceWithKeys.getDevice();

        NonceAuthentication nonceAuthentication = nonceAuthHelper
                .createNonceAuthentication(deviceWithKeys.getKeys());

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, nonceAuthentication.getNonceBase64());
        headers.add(MoreHttpHeaders.AMV_SIGNATURE, nonceAuthentication.getNonceSignatureBase64());

        HttpEntity<CreateDeviceCertificateRequestDto> entity = new HttpEntity<>(headers);

        ResponseEntity<GetAccessCertificatesResponseDto> responseEntity = restTemplate
                .exchange("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        HttpMethod.GET, entity, GetAccessCertificatesResponseDto.class,
                        device.getSerialNumber());

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertThat(responseEntity.getBody(), is(notNullValue()));

        GetAccessCertificatesResponseDto body = responseEntity.getBody();
        assertThat(body.getAccessCertificates(), is(empty()));
    }


    @Test
    @Ignore("not implemented yet")
    public void itShouldRevokeAccessCertificate() throws Exception {
        // TODO: implement me
    }


    @Test
    public void itShouldCreateAccessCertificate() throws Exception {
        VehicleEntity vehicle = demoService.getOrCreateDemoVehicle();
        DeviceEntity device = deviceWithKeys.getDevice();

        CreateAccessCertificateRequestDto request = CreateAccessCertificateRequestDto.builder()
                .appId(application.getAppId())
                .deviceSerialNumber(device.getSerialNumber())
                .vehicleSerialNumber(vehicle.getSerialNumber())
                .build();


        NonceAuthentication issuerNonceAuthentication = nonceAuthHelper
                .createNonceAuthentication(demoIssuer.getKeys());

        HttpHeaders postRequestHeaders = new HttpHeaders();
        postRequestHeaders.add(MoreHttpHeaders.AMV_NONCE, issuerNonceAuthentication.getNonceBase64());
        postRequestHeaders.add(MoreHttpHeaders.AMV_SIGNATURE, issuerNonceAuthentication.getNonceSignatureBase64());

        HttpEntity<CreateAccessCertificateRequestDto> postEntity = new HttpEntity<>(request, postRequestHeaders);

        ResponseEntity<CreateAccessCertificateResponseDto> createAccessCertificateResponse = restTemplate
                .postForEntity("/api/v1/issuer/{issuerUuid}/access_certificates",
                        postEntity,
                        CreateAccessCertificateResponseDto.class,
                        demoIssuer.getIssuer().getUuid());

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
    public void itShouldCreateAndThenSignAccessCertificate() throws Exception {
        VehicleEntity vehicle = demoService.getOrCreateDemoVehicle();
        DeviceEntity device = deviceWithKeys.getDevice();

        CreateAccessCertificateRequestDto postBody = CreateAccessCertificateRequestDto.builder()
                .appId(application.getAppId())
                .deviceSerialNumber(device.getSerialNumber())
                .vehicleSerialNumber(vehicle.getSerialNumber())
                .build();

        NonceAuthentication issuerNonceAuthentication = nonceAuthHelper
                .createNonceAuthentication(demoIssuer.getKeys());

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, issuerNonceAuthentication.getNonceBase64());
        headers.add(MoreHttpHeaders.AMV_SIGNATURE, issuerNonceAuthentication.getNonceSignatureBase64());

        HttpEntity<CreateAccessCertificateRequestDto> postEntity = new HttpEntity<>(postBody, headers);

        ResponseEntity<CreateAccessCertificateResponseDto> createAccessCertificateResponse = restTemplate
                .postForEntity("/api/v1/issuer/{issuerUuid}/access_certificates",
                        postEntity,
                        CreateAccessCertificateResponseDto.class,
                        demoIssuer.getIssuer().getUuid());

        assertThat(createAccessCertificateResponse.getStatusCode(), is(HttpStatus.OK));

        CreateAccessCertificateResponseDto postResponse = createAccessCertificateResponse.getBody();
        assertThat(postResponse, is(notNullValue()));

        AccessCertificateSigningRequestDto signingRequest = postResponse.getAccessCertificateSigningRequest();

        assertThat(signingRequest.getDeviceAccessCertificate(), is(notNullValue()));
        assertThat(isBase64(signingRequest.getDeviceAccessCertificate()), is(true));

        assertThat(signingRequest.getVehicleAccessCertificate(), is(notNullValue()));
        assertThat(isBase64(signingRequest.getVehicleAccessCertificate()), is(true));


        String vehicleAccessCertSignatureBase64 = Optional.ofNullable(cryptotool
                .generateSignature(decodeBase64AsHex(signingRequest.getVehicleAccessCertificate()),
                        demoIssuer.getKeys().getPrivateKey()))
                .map(Mono::block)
                .map(Cryptotool.Signature::getSignature)
                .map(CryptotoolUtils::encodeHexAsBase64)
                .orElseThrow(IllegalStateException::new);

        String deviceAccessCertSignatureBase64 = Optional.ofNullable(cryptotool
                .generateSignature(decodeBase64AsHex(signingRequest.getDeviceAccessCertificate()),
                        demoIssuer.getKeys().getPrivateKey()))
                .map(Mono::block)
                .map(Cryptotool.Signature::getSignature)
                .map(CryptotoolUtils::encodeHexAsBase64)
                .orElseThrow(IllegalStateException::new);

        UpdateAccessCertificateSignatureRequestDto putBody = UpdateAccessCertificateSignatureRequestDto.builder()
                .deviceAccessCertificateSignatureBase64(deviceAccessCertSignatureBase64)
                .vehicleAccessCertificateSignatureBase64(vehicleAccessCertSignatureBase64)
                .build();

        HttpEntity<UpdateAccessCertificateSignatureRequestDto> putEntity = new HttpEntity<>(putBody, headers);

        ResponseEntity<Boolean> addAccessCertificateSignatureResponse = restTemplate
                .exchange("/api/v1/issuer/{issuerUuid}/access_certificates/{accessCertificateId}/signature",
                        HttpMethod.PUT,
                        putEntity,
                        Boolean.class,
                        demoIssuer.getIssuer().getUuid(),
                        signingRequest.getId());

        assertThat(createAccessCertificateResponse.getStatusCode(), is(HttpStatus.OK));

        Boolean putResponse = addAccessCertificateSignatureResponse.getBody();
        assertThat(putResponse, is(notNullValue()));
        assertThat(putResponse, equalTo(true));
    }

    @Test
    public void itShouldCreateAndThenFetchAllAccessCertificates() throws Exception {
        itShouldCreateAndThenSignAccessCertificate();

        DeviceEntity device = deviceWithKeys.getDevice();

        NonceAuthentication deviceNonceAuthentication = nonceAuthHelper
                .createNonceAuthentication(deviceWithKeys.getKeys());

        HttpHeaders fetchRequestHeaders = new HttpHeaders();
        fetchRequestHeaders.add(MoreHttpHeaders.AMV_NONCE, deviceNonceAuthentication.getNonceBase64());
        fetchRequestHeaders.add(MoreHttpHeaders.AMV_SIGNATURE, deviceNonceAuthentication.getNonceSignatureBase64());

        HttpEntity<CreateDeviceCertificateRequestDto> fetchEntity = new HttpEntity<>(fetchRequestHeaders);

        ResponseEntity<GetAccessCertificatesResponseDto> getAccessCertificateResponse = restTemplate
                .exchange("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        HttpMethod.GET, fetchEntity, GetAccessCertificatesResponseDto.class,
                        device.getSerialNumber());

        assertThat(getAccessCertificateResponse.getStatusCode(), is(HttpStatus.OK));

        GetAccessCertificatesResponseDto body = getAccessCertificateResponse.getBody();
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
        String deviceAccessSignatureInHex = signedDeviceAccessCertificateInHex.substring(200, signedDeviceAccessCertificateInHex.length());

        String issuerPublicKeyInHex = decodeBase64AsHex(demoIssuer.getIssuer().getPublicKeyBase64());
        Cryptotool.Validity validity = Optional.of(cryptotool.verifySignature(deviceAccessCertificateInHex, deviceAccessSignatureInHex, issuerPublicKeyInHex))
                .map(Mono::block)
                .filter(val -> val == Cryptotool.Validity.VALID)
                .orElseThrow(IllegalStateException::new);

        assertThat(validity, is(Cryptotool.Validity.VALID));
    }
}
