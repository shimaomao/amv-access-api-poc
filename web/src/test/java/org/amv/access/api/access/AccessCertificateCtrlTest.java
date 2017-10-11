package org.amv.access.api.access;

import org.amv.access.AmvAccessApplication;
import org.amv.access.api.MoreHttpHeaders;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.GetAccessCertificatesResponseDto;
import org.amv.access.config.TestDbConfig;
import org.amv.access.model.*;
import org.amv.access.test.DeviceWithKeys;
import org.amv.access.util.MoreBase64;
import org.amv.access.util.SecureRandomUtils;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
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

import java.security.SecureRandom;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.Matchers.is;
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
    private VehicleRepository vehicleRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    private ApplicationEntity application;
    private DeviceWithKeys deviceWithKeys;

    @Before
    public void setUp() {
        this.application = applicationRepository.save(ApplicationEntity.builder()
                .name("Test ApplicationEntity")
                .appId(CryptotoolUtils.TestUtils.generateRandomAppId())
                .apiKey(RandomStringUtils.randomAlphanumeric(8))
                .enabled(true)
                .build());

        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        DeviceEntity device = createAndSaveDevice(application, keys);

        this.deviceWithKeys = DeviceWithKeys.builder()
                .device(device)
                .keys(keys)
                .build();
    }

    @Test
    public void itShouldFailGetAccessCertificateIfNonceHeaderIsMissing() throws Exception {
        DeviceEntity device = deviceWithKeys.getDevice();

        HttpHeaders headers = new HttpHeaders();

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<?> responseEntity = restTemplate
                .exchange("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        HttpMethod.GET, entity, GetAccessCertificatesResponseDto.class,
                        device.getSerialNumber());

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void itShouldFailGetAccessCertificateIfSignatureHeaderIsMissing() throws Exception {
        DeviceEntity device = deviceWithKeys.getDevice();

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, "42");

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<?> responseEntity = restTemplate
                .exchange("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        HttpMethod.GET, entity, GetAccessCertificatesResponseDto.class,
                        device.getSerialNumber());

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }


    @Test
    public void itShouldFailGetAccessCertificateIfNonceSignatureHeaderIsInvalid() throws Exception {
        DeviceEntity device = deviceWithKeys.getDevice();

        String nonce = generateNonceWithRandomLength();
        String signedNonce = signNonce(deviceWithKeys.getKeys(), generateNonceWithRandomLength());

        String devicePublicKey = MoreBase64.fromBase64(device.getPublicKeyBase64())
                .orElseThrow(IllegalStateException::new);
        Cryptotool.Validity signedNonceValidity = Optional.of(cryptotool.verifySignature(nonce, signedNonce, devicePublicKey))
                .map(Mono::block)
                .orElse(Cryptotool.Validity.VALID);

        assertThat("Sanity check", signedNonceValidity, is(Cryptotool.Validity.INVALID));

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, nonce);
        headers.add(MoreHttpHeaders.AMV_SIGNATURE, signedNonce);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<?> responseEntity = restTemplate
                .exchange("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        HttpMethod.GET, entity, GetAccessCertificatesResponseDto.class,
                        device.getSerialNumber());

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

    @Test
    public void itShouldGetAccessCertificatesSuccessfully() throws Exception {
        DeviceEntity device = deviceWithKeys.getDevice();

        String nonce = generateNonceWithRandomLength();
        String signedNonce = signNonce(deviceWithKeys.getKeys(), nonce);

        String devicePublicKey = MoreBase64.fromBase64(device.getPublicKeyBase64())
                .orElseThrow(IllegalStateException::new);
        Cryptotool.Validity signedNonceValidity = Optional.of(cryptotool.verifySignature(nonce, signedNonce, devicePublicKey))
                .map(Mono::block)
                .orElse(Cryptotool.Validity.INVALID);

        assertThat("Sanity check", signedNonceValidity, is(Cryptotool.Validity.VALID));

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, nonce);
        headers.add(MoreHttpHeaders.AMV_SIGNATURE, signedNonce);

        HttpEntity<CreateDeviceCertificateRequestDto> entity = new HttpEntity<>(headers);

        ResponseEntity<GetAccessCertificatesResponseDto> responseEntity = restTemplate
                .exchange("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        HttpMethod.GET, entity, GetAccessCertificatesResponseDto.class,
                        device.getSerialNumber());

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        // TODO: test for present access certificates
    }


    @Test
    @Ignore("not implemented yet")
    public void itShouldRevokeAccessCertificate() throws Exception {
        // TODO: implement me
    }

    /*
    @Test
    public void itShouldCreateAccessCertificate() throws Exception {
        VehicleEntity vehicle = createDummyVehicle();
        DeviceEntity device = createDummyDevice();

        CreateAccessCertificateRequest request = CreateAccessCertificateRequest.builder()
                .appId(device.getAppId())
                .deviceSerialNumber(device.getSerialNumber())
                .vehicleSerialNumber(vehicle.getSerialNumber())
                .build();

        ResponseEntity<CreateAccessCertificateResponse> responseEntity = restTemplate
                .postForEntity("/api/v1/access_certificates", request,
                        CreateAccessCertificateResponse.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));

        CreateAccessCertificateResponse response = responseEntity.getBody();
        assertThat(response, is(notNullValue()));

        AccessCertificateDto accessCertificate = response.getAccessCertificate();

        assertThat(accessCertificate.getDeviceAccessCertificate(), is(notNullValue()));
        assertThat(isBase64(accessCertificate.getDeviceAccessCertificate()), is(true));

        assertThat(accessCertificate.getVehicleAccessCertificate(), is(notNullValue()));
        assertThat(isBase64(accessCertificate.getVehicleAccessCertificate()), is(true));
    }

    @Test
    public void itShouldCreateAndThenFetchAllAccessCertificates() throws Exception {
        VehicleEntity vehicle = createDummyVehicle();
        DeviceEntity device = createDummyDevice();

        CreateAccessCertificateRequest createAccessCertificateRequest = CreateAccessCertificateRequest.builder()
                .appId(device.getAppId())
                .deviceSerialNumber(device.getSerialNumber())
                .vehicleSerialNumber(vehicle.getSerialNumber())
                .build();

        ResponseEntity<CreateAccessCertificateResponse> createAccessCertificateResponse = restTemplate
                .postForEntity("/api/v1/access_certificates", createAccessCertificateRequest,
                        CreateAccessCertificateResponse.class);

        assertThat(createAccessCertificateResponse.getStatusCode(), is(HttpStatus.OK));

        GetAccessCertificateRequest getAccessCertificateRequest = GetAccessCertificateRequest.builder()
                .accessGainingSerialNumber(device.getSerialNumber())
                .accessProvidingSerialNumber(vehicle.getSerialNumber())
                .build();

        ResponseEntity<GetAccessCertificatesResponseDto> getAccessCertificateResponse = restTemplate
                .getForEntity("/api/v1/access_certificates?" +
                        "accessGainingSerialNumber={accessGainingSerialNumber}&" +
                        "accessProvidingSerialNumber={accessProvidingSerialNumber}", GetAccessCertificatesResponseDto.class,
                        getAccessCertificateRequest.getAccessGainingSerialNumber(),
                        getAccessCertificateRequest.getAccessProvidingSerialNumber());

        assertThat(getAccessCertificateResponse.getStatusCode(), is(HttpStatus.OK));

        GetAccessCertificatesResponseDto body = getAccessCertificateResponse.getBody();
        assertThat(body, is(notNullValue()));
        assertThat(body.getAccessCertificate(), is(notNullValue()));
        assertThat(body.getAccessCertificate().getDeviceAccessCertificate(), is(notNullValue()));
        assertThat(body.getAccessCertificate().getVehicleAccessCertificate(), is(notNullValue()));
    }*/


    private String generateNonceWithRandomLength() {
        return generateNonce(RandomUtils.nextInt(8, 32));
    }

    private String generateNonce(int numberOfBytes) {
        checkArgument(numberOfBytes > 0);
        SecureRandom random = new SecureRandom();
        byte nonceBytes[] = new byte[numberOfBytes];
        random.nextBytes(nonceBytes);

        return Hex.encodeHexString(nonceBytes);
    }

    private String signNonce(Cryptotool.Keys keys, String nonce) {
        return Optional.of(cryptotool.generateSignature(nonce, keys.getPrivateKey()))
                .map(Mono::block)
                .map(Cryptotool.Signature::getSignature)
                .orElseThrow(IllegalStateException::new);
    }

    private DeviceEntity createAndSaveDevice(ApplicationEntity application, Cryptotool.Keys keys) {
        String devicePublicKeyBase64 = MoreBase64.toBase64(keys.getPublicKey())
                .orElseThrow(IllegalStateException::new);

        return deviceRepository.save(DeviceEntity.builder()
                .applicationId(application.getId())
                .name(StringUtils.prependIfMissing("test-", RandomStringUtils.randomAlphanumeric(10)))
                .serialNumber(SecureRandomUtils.generateRandomSerial())
                .publicKeyBase64(devicePublicKeyBase64)
                .build());
    }
}