package org.amv.access.api.access;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import feign.FeignException;
import org.amv.access.AmvAccessApplication;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.client.AccessCertClient;
import org.amv.access.client.Clients;
import org.amv.access.client.model.CreateAccessCertificateRequestDto;
import org.amv.access.client.model.CreateAccessCertificateResponseDto;
import org.amv.access.client.model.CreateAccessCertificateResponseDto.AccessCertificateSigningRequestDto;
import org.amv.access.client.model.GetAccessCertificatesResponseDto;
import org.amv.access.config.SqliteTestDatabaseConfig;
import org.amv.access.demo.DemoService;
import org.amv.access.demo.DeviceWithKeys;
import org.amv.access.demo.IssuerWithKeys;
import org.amv.access.demo.NonceAuthHelper;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.model.DeviceEntity;
import org.amv.access.model.VehicleEntity;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.ServletContext;

import static org.apache.commons.codec.binary.Base64.isBase64;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AmvAccessApplication.class, SqliteTestDatabaseConfig.class}
)
public class AccessCertificateCtrlWithRestClient {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private Cryptotool cryptotool;

    @Autowired
    private DemoService demoService;

    private ApplicationEntity application;

    private DeviceWithKeys deviceWithKeys;
    private IssuerWithKeys demoIssuer;
    private NonceAuthHelper nonceAuthHelper;
    private AccessCertClient accessCertClient;

    @Before
    public void setUp() {
        this.nonceAuthHelper = new NonceAuthHelper(cryptotool);
        this.application = demoService.getOrCreateDemoApplication();

        this.demoIssuer = demoService.getOrCreateDemoIssuer();

        this.deviceWithKeys = demoService.createDemoDeviceWithKeys(this.application);

        String baseUrl = String.format("http://localhost:%d/%s", port, servletContext.getContextPath());
        this.accessCertClient = Clients.simpleAccessCertClient(baseUrl);
    }

    @Test
    public void itShouldFailGetAccessCertificateIfNonceHeaderIsMissing() throws Exception {
        DeviceEntity device = deviceWithKeys.getDevice();

        try {
            GetAccessCertificatesResponseDto execute = accessCertClient
                    .fetchAccessCertificates("", "", device.getSerialNumber())
                    .execute();
            Assert.fail("Should have thrown exception.");
        } catch (HystrixRuntimeException e) {
            assertThat(e.getCause(), is(notNullValue()));
            assertThat(e.getCause(), instanceOf(FeignException.class));
            assertThat(((FeignException) e.getCause()).status(), is(HttpStatus.BAD_REQUEST.value()));
        }
    }

    @Test
    public void itShouldGetEmptyAccessCertificateListSuccessfully() throws Exception {
        DeviceEntity device = deviceWithKeys.getDevice();

        NonceAuthentication nonceAuthentication = nonceAuthHelper
                .createNonceAuthentication(deviceWithKeys.getKeys());

        GetAccessCertificatesResponseDto body = accessCertClient.fetchAccessCertificates(
                nonceAuthentication.getNonceBase64(),
                nonceAuthentication.getNonceSignatureBase64(),
                device.getSerialNumber())
                .execute();

        assertThat(body, is(notNullValue()));
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

        NonceAuthentication nonceAuthentication = nonceAuthHelper
                .createNonceAuthentication(demoIssuer.getKeys());

        CreateAccessCertificateResponseDto response = accessCertClient
                .createAccessCertificate(nonceAuthentication.getNonceBase64(),
                        nonceAuthentication.getNonceSignatureBase64(),
                        demoIssuer.getIssuer().getUuid(),
                        request)
                .execute();

        assertThat(response, is(notNullValue()));

        AccessCertificateSigningRequestDto signingRequest = response.getAccessCertificateSigningRequest();

        assertThat(signingRequest.getDeviceAccessCertificate(), is(notNullValue()));
        assertThat(isBase64(signingRequest.getDeviceAccessCertificate()), is(true));

        assertThat(signingRequest.getVehicleAccessCertificate(), is(notNullValue()));
        assertThat(isBase64(signingRequest.getVehicleAccessCertificate()), is(true));
    }

/*
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
                .validityStart(validFrom.toInstant(ZoneOffset.UTC))
                .validityEnd(validUntil.toInstant(ZoneOffset.UTC))
                .build();

        ResponseEntity<CreateAccessCertificateResponseDto> createAccessCertificateResponse = restTemplate
                .postForEntity("/api/v1/device/{deviceSerialNumber}/access_certificates",
                        createAccessCertificateRequest,
                        CreateAccessCertificateResponseDto.class,
                        device.getSerialNumber());

        assertThat(createAccessCertificateResponse.getStatusCode(), is(HttpStatus.OK));

        NonceAuthentication nonceAuthentication = nonceAuthHelper
                .createNonceAuthentication(deviceWithKeys.getKeys());

        HttpHeaders headers = new HttpHeaders();
        headers.add(MoreHttpHeaders.AMV_NONCE, nonceAuthentication.getNonceBase64());
        headers.add(MoreHttpHeaders.AMV_SIGNATURE, nonceAuthentication.getNonceSignatureBase64());

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
    }*/
}
