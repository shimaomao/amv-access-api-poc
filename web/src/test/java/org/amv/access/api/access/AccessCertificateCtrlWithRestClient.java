package org.amv.access.api.access;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import feign.FeignException;
import org.amv.access.AmvAccessApplication;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.client.AccessCertClient;
import org.amv.access.client.Clients;
import org.amv.access.client.model.*;
import org.amv.access.client.model.CreateAccessCertificateResponseDto.AccessCertificateSigningRequestDto;
import org.amv.access.config.SqliteTestDatabaseConfig;
import org.amv.access.demo.DemoService;
import org.amv.access.demo.DeviceWithKeys;
import org.amv.access.demo.IssuerWithKeys;
import org.amv.access.demo.NonceAuthHelper;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.model.DeviceEntity;
import org.amv.access.model.VehicleEntity;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

import javax.servlet.ServletContext;
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
        GetAccessCertificatesResponseDto body = executeFetchAccessCertificateRequest(deviceWithKeys);

        assertThat(body, is(notNullValue()));
        assertThat(body.getAccessCertificates(), is(empty()));
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

        CreateAccessCertificateResponseDto response = executeCreateAccessCertificateRequest(demoIssuer, request);

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

        CreateAccessCertificateRequestDto createRequest = CreateAccessCertificateRequestDto.builder()
                .appId(application.getAppId())
                .deviceSerialNumber(device.getSerialNumber())
                .vehicleSerialNumber(vehicle.getSerialNumber())
                .build();

        CreateAccessCertificateResponseDto createResponse = executeCreateAccessCertificateRequest(demoIssuer, createRequest);

        AccessCertificateSigningRequestDto signingRequest = createResponse.getAccessCertificateSigningRequest();

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

        Boolean addAccessCertificateSignatureResponse = executeAddAccessCertificateSignaturesRequest(
                demoIssuer, signingRequest.getId(), putBody
        );

        assertThat(addAccessCertificateSignatureResponse, is(notNullValue()));
        assertThat(addAccessCertificateSignatureResponse, equalTo(true));
    }

    @Test
    public void itShouldCreateAndThenFetchAllAccessCertificates() throws Exception {
        itShouldCreateAndThenSignAccessCertificate();

        GetAccessCertificatesResponseDto body = executeFetchAccessCertificateRequest(deviceWithKeys);

        List<AccessCertificateDto> accessCertificates = body.getAccessCertificates();
        assertThat(accessCertificates, is(notNullValue()));
        assertThat(accessCertificates, hasSize(1));
    }

    @Test
    public void itShouldRevokeAccessCertificate() throws Exception {
        itShouldCreateAndThenFetchAllAccessCertificates();

        List<AccessCertificateDto> accessCertificatesBeforeRevocation = executeFetchAccessCertificateRequest(deviceWithKeys)
                .getAccessCertificates();

        AccessCertificateDto firstAccessCertificate = accessCertificatesBeforeRevocation
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("There should be at least one cert by now."));

        executeRevokeAccessCertificateRequest(demoIssuer, firstAccessCertificate.getId());

        List<AccessCertificateDto> accessCertificatesAfterRevocation = executeFetchAccessCertificateRequest(deviceWithKeys)
                .getAccessCertificates();

        assertThat(accessCertificatesAfterRevocation.size() + 1, is(accessCertificatesBeforeRevocation.size()));
    }

    private GetAccessCertificatesResponseDto executeFetchAccessCertificateRequest(DeviceWithKeys deviceWithKeys) {
        NonceAuthentication nonceAuthentication = nonceAuthHelper
                .createNonceAuthentication(deviceWithKeys.getKeys());

        return accessCertClient.fetchAccessCertificates(
                nonceAuthentication.getNonceBase64(),
                nonceAuthentication.getNonceSignatureBase64(),
                deviceWithKeys.getDevice().getSerialNumber())
                .execute();
    }

    private CreateAccessCertificateResponseDto executeCreateAccessCertificateRequest(IssuerWithKeys issuerWithKeys,
                                                                                     CreateAccessCertificateRequestDto request) {
        NonceAuthentication nonceAuthentication = nonceAuthHelper
                .createNonceAuthentication(issuerWithKeys.getKeys());

        return accessCertClient.createAccessCertificate(nonceAuthentication.getNonceBase64(),
                nonceAuthentication.getNonceSignatureBase64(),
                issuerWithKeys.getIssuer().getUuid(),
                request)
                .execute();
    }


    private Boolean executeAddAccessCertificateSignaturesRequest(IssuerWithKeys issuerWithKeys,
                                                                 String accessCertificateId,
                                                                 UpdateAccessCertificateSignatureRequestDto request) {
        NonceAuthentication issuerNonceAuthentication = nonceAuthHelper
                .createNonceAuthentication(issuerWithKeys.getKeys());

        return accessCertClient.addAccessCertificateSignature(issuerNonceAuthentication.getNonceBase64(),
                issuerNonceAuthentication.getNonceSignatureBase64(),
                issuerWithKeys.getIssuer().getUuid(),
                accessCertificateId, request)
                .execute();
    }

    private Void executeRevokeAccessCertificateRequest(IssuerWithKeys issuerWithKeys,
                                                       String accessCertificateId) {
        NonceAuthentication nonceAuthentication = nonceAuthHelper
                .createNonceAuthentication(issuerWithKeys.getKeys());

        return accessCertClient.revokeAccessCertificate(nonceAuthentication.getNonceBase64(),
                nonceAuthentication.getNonceSignatureBase64(),
                issuerWithKeys.getIssuer().getUuid(),
                accessCertificateId)
                .execute();
    }

}
