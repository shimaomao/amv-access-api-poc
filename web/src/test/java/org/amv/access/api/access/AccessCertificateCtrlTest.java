package org.amv.access.api.access;

import org.amv.access.AmvAccessApplication;
import org.amv.access.api.access.model.*;
import org.amv.access.config.TestDbConfig;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.amv.access.model.Device;
import org.amv.access.model.DeviceRepository;
import org.amv.access.model.Vehicle;
import org.amv.access.model.VehicleRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
    private TestRestTemplate restTemplate;

    @Test
    public void itShouldFetchAllAccessCertificates() throws Exception {
        Vehicle vehicle = createDummyVehicle();
        Device device = createDummyDevice();

        GetAccessCertificateRequest request = GetAccessCertificateRequest.builder()
                .accessGainingSerialNumber(device.getSerialNumber())
                .accessProvidingSerialNumber(vehicle.getSerialNumber())
                .build();

        ResponseEntity<GetAccessCertificateResponse> responseEntity = restTemplate
                .getForEntity("/api/v1/access_certificates?" +
                                "accessGainingSerialNumber={accessGainingSerialNumber}&" +
                                "accessProvidingSerialNumber={accessProvidingSerialNumber}", GetAccessCertificateResponse.class,
                        request.getAccessGainingSerialNumber(),
                        request.getAccessProvidingSerialNumber());

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

    @Test
    public void itShouldCreateAccessCertificate() throws Exception {
        Vehicle vehicle = createDummyVehicle();
        Device device = createDummyDevice();

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
        Vehicle vehicle = createDummyVehicle();
        Device device = createDummyDevice();

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

        ResponseEntity<GetAccessCertificateResponse> getAccessCertificateResponse = restTemplate
                .getForEntity("/api/v1/access_certificates?" +
                        "accessGainingSerialNumber={accessGainingSerialNumber}&" +
                        "accessProvidingSerialNumber={accessProvidingSerialNumber}", GetAccessCertificateResponse.class,
                        getAccessCertificateRequest.getAccessGainingSerialNumber(),
                        getAccessCertificateRequest.getAccessProvidingSerialNumber());

        assertThat(getAccessCertificateResponse.getStatusCode(), is(HttpStatus.OK));

        GetAccessCertificateResponse body = getAccessCertificateResponse.getBody();
        assertThat(body, is(notNullValue()));
        assertThat(body.getAccessCertificate(), is(notNullValue()));
        assertThat(body.getAccessCertificate().getDeviceAccessCertificate(), is(notNullValue()));
        assertThat(body.getAccessCertificate().getVehicleAccessCertificate(), is(notNullValue()));
    }

    private Device createDummyDevice() {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        Device device = Device.builder()
                .appId(CryptotoolUtils.TestUtils.generateRandomAppId())
                .name(StringUtils.prependIfMissing("test-", RandomStringUtils.randomAlphanumeric(10)))
                .serialNumber(CryptotoolUtils.TestUtils.generateRandomSerial())
                .publicKey(keys.getPublicKey())
                .build();

        return deviceRepository.save(device);
    }

    private Vehicle createDummyVehicle() {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        Vehicle vehicle = Vehicle.builder()
                .serialNumber(CryptotoolUtils.TestUtils.generateRandomSerial())
                .publicKey(keys.getPublicKey())
                .build();

        return vehicleRepository.save(vehicle);
    }

    @Test
    @Ignore("not implemented yet")
    public void itShouldRevokeAccessCertificate() throws Exception {
        // TODO: implement me
    }

}