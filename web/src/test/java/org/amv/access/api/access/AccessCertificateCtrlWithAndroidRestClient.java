package org.amv.access.api.access;

import org.amv.access.AmvAccessApplication;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.client.android.AccessApiException;
import org.amv.access.client.android.AccessCertClient;
import org.amv.access.client.android.Clients;
import org.amv.access.client.android.model.ErrorResponseDto;
import org.amv.access.client.android.model.GetAccessCertificatesResponseDto;
import org.amv.access.config.SqliteTestDatabaseConfig;
import org.amv.access.demo.DemoService;
import org.amv.access.demo.DeviceWithKeys;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.model.DeviceEntity;
import org.amv.access.spi.highmobility.NonceAuthenticationService;
import org.amv.access.spi.highmobility.NonceAuthenticationServiceImpl;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.ServletContext;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AmvAccessApplication.class, SqliteTestDatabaseConfig.class}
)
public class AccessCertificateCtrlWithAndroidRestClient {

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
    private NonceAuthenticationService nonceAuthService;
    private AccessCertClient accessCertClient;

    @Before
    public void setUp() {
        this.nonceAuthService = new NonceAuthenticationServiceImpl(cryptotool);
        this.application = demoService.getOrCreateDemoApplication();

        this.deviceWithKeys = demoService.createDemoDeviceWithKeys(this.application);

        String baseUrl = String.format("http://localhost:%d/%s", port, servletContext.getContextPath());
        this.accessCertClient = Clients.simpleAccessCertClient(baseUrl);
    }

    @Test
    public void itShouldFailGetAccessCertificateIfNonceHeaderIsMissing() throws Exception {
        DeviceEntity device = deviceWithKeys.getDevice();

        try {
            GetAccessCertificatesResponseDto execute = accessCertClient
                    .fetchAccessCertificates("", "", device.getSerialNumber().toHex())
                    .blockingSingle();

            Assert.fail("Should have thrown exception.");
        } catch (Exception e) {
            assertThat(e.getCause(), is(notNullValue()));
            assertThat(e.getCause(), instanceOf(AccessApiException.class));
            ErrorResponseDto errorResponseDto = ((AccessApiException) e.getCause()).getError();

            List<ErrorResponseDto.ErrorInfoDto> errors = errorResponseDto.errors;
            assertThat(errors, hasSize(greaterThanOrEqualTo(1)));
            ErrorResponseDto.ErrorInfoDto errorInfoDto = errors.get(0);
            assertThat(errorInfoDto.title, is("BadRequestException"));
            assertThat(errorInfoDto.detail, is("amv-api-nonce header is missing"));
        }
    }

    @Test
    public void itShouldGetEmptyAccessCertificateListSuccessfully() throws Exception {
        GetAccessCertificatesResponseDto body = executeFetchAccessCertificateRequest(deviceWithKeys);

        assertThat(body, is(notNullValue()));
        assertThat(body.access_certificates, is(empty()));
    }

    private GetAccessCertificatesResponseDto executeFetchAccessCertificateRequest(DeviceWithKeys deviceWithKeys) {
        NonceAuthentication nonceAuthentication = nonceAuthService
                .createNonceAuthentication(deviceWithKeys.getPrivateKey());

        return accessCertClient.fetchAccessCertificates(
                nonceAuthentication.getNonceBase64(),
                nonceAuthentication.getNonceSignatureBase64(),
                deviceWithKeys.getDevice().getSerialNumber().toHex()
        ).blockingSingle();
    }
}
