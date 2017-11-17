package org.amv.access.api.access;

import org.amv.access.AmvAccessApplication;
import org.amv.access.api.access.model.CreateAccessCertificateRequestDto;
import org.amv.access.api.access.model.CreateAccessCertificateResponseDto;
import org.amv.access.client.MoreHttpHeaders;
import org.amv.access.client.model.AccessCertificateDto;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.ErrorResponseDto;
import org.amv.access.client.model.GetAccessCertificatesResponseDto;
import org.amv.access.config.TestDbConfig;
import org.amv.access.demo.DemoService;
import org.amv.access.demo.DeviceWithKeys;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.UnauthorizedException;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.model.DeviceEntity;
import org.amv.access.model.IssuerEntity;
import org.amv.access.model.VehicleEntity;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.apache.commons.lang3.RandomUtils;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.codec.binary.Base64.isBase64;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AmvAccessApplication.class, TestDbConfig.class}
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
    private IssuerEntity demoIssuer;

    @Before
    public void setUp() {
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

        String nonceBase64 = generateNonceWithRandomLengthBase64();
        String nonceSignatureBase64 = createNonceSignatureBase64(deviceWithKeys.getKeys(), generateNonceWithRandomLengthBase64());

        String devicePublicKey = CryptotoolUtils.decodeBase64AsHex(device.getPublicKeyBase64());
        Cryptotool.Validity signedNonceValidity = Optional.of(cryptotool.verifySignature(
                CryptotoolUtils.decodeBase64AsHex(nonceBase64),
                CryptotoolUtils.decodeBase64AsHex(nonceSignatureBase64),
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

        String nonceBase64 = generateNonceWithRandomLengthBase64();
        String nonceSignatureBase64 = createNonceSignatureBase64(deviceWithKeys.getKeys(), CryptotoolUtils.decodeBase64AsHex(nonceBase64));

        String devicePublicKey = CryptotoolUtils.decodeBase64AsHex(device.getPublicKeyBase64());
        Cryptotool.Validity signedNonceValidity = Optional.of(cryptotool.verifySignature(
                CryptotoolUtils.decodeBase64AsHex(nonceBase64),
                CryptotoolUtils.decodeBase64AsHex(nonceSignatureBase64),
                devicePublicKey))
                .map(Mono::block)
                .orElse(Cryptotool.Validity.INVALID);

        assertThat("Sanity check", signedNonceValidity, is(Cryptotool.Validity.VALID));

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, nonceBase64);
        headers.add(MoreHttpHeaders.AMV_SIGNATURE, nonceSignatureBase64);

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

        ResponseEntity<CreateAccessCertificateResponseDto> responseEntity = restTemplate
                .postForEntity("/api/v1/device/{deviceSerialNumber}/access_certificates", request,
                        CreateAccessCertificateResponseDto.class,
                        device.getSerialNumber());

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));

        CreateAccessCertificateResponseDto response = responseEntity.getBody();
        assertThat(response, is(notNullValue()));

        AccessCertificateDto accessCertificate = response.getAccessCertificate();

        assertThat(accessCertificate.getDeviceAccessCertificate(), is(notNullValue()));
        assertThat(isBase64(accessCertificate.getDeviceAccessCertificate()), is(true));

        assertThat(accessCertificate.getVehicleAccessCertificate(), is(notNullValue()));
        assertThat(isBase64(accessCertificate.getVehicleAccessCertificate()), is(true));
    }

    @Test
    public void itShouldCreateAndThenFetchAllAccessCertificates() throws Exception {
        VehicleEntity vehicle = demoService.getOrCreateDemoVehicle();
        DeviceEntity device = deviceWithKeys.getDevice();

        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validUntil = validFrom.plusMinutes(RandomUtils.nextInt(10, 1_000_000));

        CreateAccessCertificateRequestDto createAccessCertificateRequest = CreateAccessCertificateRequestDto.builder()
                .appId(application.getAppId())
                .deviceSerialNumber(device.getSerialNumber())
                .vehicleSerialNumber(vehicle.getSerialNumber())
                .validityStart(validFrom)
                .validityEnd(validUntil)
                .build();

        ResponseEntity<CreateAccessCertificateResponseDto> createAccessCertificateResponse = restTemplate
                .postForEntity("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        createAccessCertificateRequest,
                        CreateAccessCertificateResponseDto.class,
                        device.getSerialNumber());

        assertThat(createAccessCertificateResponse.getStatusCode(), is(HttpStatus.OK));

        String nonceBase64 = generateNonceWithRandomLengthBase64();
        String nonceSignatureBase64 = createNonceSignatureBase64(deviceWithKeys.getKeys(), CryptotoolUtils.decodeBase64AsHex(nonceBase64));

        String devicePublicKey = CryptotoolUtils.decodeBase64AsHex(device.getPublicKeyBase64());

        Cryptotool.Validity signedNonceValidity = Optional.of(cryptotool.verifySignature(
                CryptotoolUtils.decodeBase64AsHex(nonceBase64),
                CryptotoolUtils.decodeBase64AsHex(nonceSignatureBase64),
                devicePublicKey))
                .map(Mono::block)
                .orElse(Cryptotool.Validity.INVALID);

        assertThat("Sanity check", signedNonceValidity, is(Cryptotool.Validity.VALID));

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, nonceBase64);
        headers.add(MoreHttpHeaders.AMV_SIGNATURE, nonceSignatureBase64);

        HttpEntity<CreateDeviceCertificateRequestDto> entity = new HttpEntity<>(headers);

        ResponseEntity<GetAccessCertificatesResponseDto> getAccessCertificateResponse = restTemplate
                .exchange("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        HttpMethod.GET, entity, GetAccessCertificatesResponseDto.class,
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

        String signedDeviceAccessCertificateInHex = CryptotoolUtils.decodeBase64AsHex(accessCertificateDto.getDeviceAccessCertificate());

        String deviceAccessCertificateInHex = signedDeviceAccessCertificateInHex.substring(0, 200);
        String deviceAccessSignatureInHex = signedDeviceAccessCertificateInHex.substring(200, signedDeviceAccessCertificateInHex.length());

        String issuerPublicKeyInHex = CryptotoolUtils.decodeBase64AsHex(demoIssuer.getPublicKeyBase64());
        Cryptotool.Validity validity = Optional.of(cryptotool.verifySignature(deviceAccessCertificateInHex, deviceAccessSignatureInHex, issuerPublicKeyInHex))
                .map(Mono::block)
                .filter(val -> val == Cryptotool.Validity.VALID)
                .orElseThrow(IllegalStateException::new);

        assertThat(validity, is(Cryptotool.Validity.VALID));
    }

    private String generateNonceWithRandomLengthBase64() {
        return generateNonceBase64(RandomUtils.nextInt(8, 32));
    }

    private String generateNonceBase64(int numberOfBytes) {
        return CryptotoolUtils.SecureRandomUtils.generateRandomHexString(numberOfBytes);
    }

    private String createNonceSignatureBase64(Cryptotool.Keys keys, String nonce) {
        return Optional.of(cryptotool.generateSignature(nonce, keys.getPrivateKey()))
                .map(Mono::block)
                .map(Cryptotool.Signature::getSignature)
                .map(CryptotoolUtils::encodeHexAsBase64)
                .orElseThrow(IllegalStateException::new);
    }
}
