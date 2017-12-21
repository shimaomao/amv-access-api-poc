package org.amv.access.api.device;

import org.amv.access.AmvAccessApplication;
import org.amv.access.client.android.AccessApiException;
import org.amv.access.client.android.Clients;
import org.amv.access.client.android.DeviceCertClient;
import org.amv.access.client.android.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.android.model.CreateDeviceCertificateResponseDto;
import org.amv.access.client.android.model.DeviceCertificateDto;
import org.amv.access.client.android.model.ErrorResponseDto;
import org.amv.access.config.SqliteTestDatabaseConfig;
import org.amv.access.core.Issuer;
import org.amv.access.demo.DemoService;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.util.MoreHex;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

import javax.servlet.ServletContext;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.codec.binary.Base64.isBase64;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AmvAccessApplication.class, SqliteTestDatabaseConfig.class}
)
public class DeviceCertificateCtrlTestWithAndroidRestClient {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private Cryptotool cryptotool;

    @Autowired
    private DemoService demoService;

    @Autowired
    private Issuer issuer;

    private DeviceCertClient deviceCertClient;

    private ApplicationEntity application;

    @Before
    public void setUp() {
        this.application = demoService.getOrCreateDemoApplication();

        String baseUrl = String.format("http://localhost:%d/%s", port, servletContext.getContextPath());
        this.deviceCertClient = Clients.simpleDeviceCertClient(baseUrl);
    }

    @Test
    public void itShouldFailCreatingDeviceCertificateIfApplicationDoesNotExist() throws Exception {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();
        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        CreateDeviceCertificateRequestDto body = new CreateDeviceCertificateRequestDto();
        body.device_public_key = publicKeyBase64;

        String appId = application.getAppId().toHex();
        String nonExistingApiKey = application.getApiKey() + "123";

        try {
            this.deviceCertClient
                    .createDeviceCertificate(appId, nonExistingApiKey, body)
                    .blockingSingle();

            Assert.fail("Should have thrown exception.");
        } catch (Exception e) {
            assertThat(e.getCause(), is(notNullValue()));
            assertThat(e.getCause(), instanceOf(AccessApiException.class));
            ErrorResponseDto errorResponseDto = ((AccessApiException) e.getCause()).getError();

            List<ErrorResponseDto.ErrorInfoDto> errors = errorResponseDto.errors;
            assertThat(errors, hasSize(greaterThanOrEqualTo(1)));
            ErrorResponseDto.ErrorInfoDto errorInfoDto = errors.get(0);
            assertThat(errorInfoDto.title, is("UnauthorizedException"));
            assertThat(errorInfoDto.detail, is("ApplicationEntity not found"));
        }
    }

    @Test
    public void itShouldCreateDeviceCertificateSuccessfully() throws Exception {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();
        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        CreateDeviceCertificateRequestDto body = new CreateDeviceCertificateRequestDto();
        body.device_public_key = publicKeyBase64;

        CreateDeviceCertificateResponseDto response = this.deviceCertClient
                .createDeviceCertificate(application.getAppId().toHex(), application.getApiKey(), body)
                .blockingSingle();

        assertThat(response, is(notNullValue()));

        DeviceCertificateDto responseDeviceCertificate = response.device_certificate;
        assertThat(responseDeviceCertificate, is(notNullValue()));

        String expectedIssuerPublicKey = issuer.getPublicKey().toBase64();
        assertThat(responseDeviceCertificate.issuer_public_key, is(expectedIssuerPublicKey));

        String signedDeviceCertificate = responseDeviceCertificate.device_certificate;
        assertThat(signedDeviceCertificate, is(notNullValue()));
        assertThat(isBase64(signedDeviceCertificate), is(true));

        String signedDeviceCertificateInHex = CryptotoolUtils.decodeBase64AsHex(signedDeviceCertificate);

        String deviceCertificate = signedDeviceCertificateInHex.substring(0, 178);
        String signatureInHex = signedDeviceCertificateInHex.substring(178, signedDeviceCertificateInHex.length());

        String issuerPublicKeyInHex = issuer.getPublicKey().toHex();
        Cryptotool.Validity validity = Optional.of(cryptotool.verifySignature(deviceCertificate, signatureInHex, issuerPublicKeyInHex))
                .map(Mono::block)
                .filter(val -> val == Cryptotool.Validity.VALID)
                .orElseThrow(IllegalStateException::new);

        assertThat(validity, is(Cryptotool.Validity.VALID));

        String issuerNameInHex = deviceCertificate.substring(0, 8);
        String applicationIdInHex = deviceCertificate.substring(8, 32);
        String deviceSerialNumberInHex = deviceCertificate.substring(32, 50);
        String devicePublicKeyInHex = deviceCertificate.substring(50, deviceCertificate.length());

        assertThat(issuerNameInHex, is(issuer.getNameInHex()));
        assertThat(applicationIdInHex, is(application.getAppId().toHex()));
        assertThat(MoreHex.isHex(deviceSerialNumberInHex), is(true));
        assertThat(devicePublicKeyInHex, is(equalToIgnoringCase(keys.getPublicKey())));
    }
}
