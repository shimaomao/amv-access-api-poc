package org.amv.access.api.device;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import feign.FeignException;
import org.amv.access.AmvAccessApplication;
import org.amv.access.client.Clients;
import org.amv.access.client.DeviceCertClient;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.CreateDeviceCertificateResponseDto;
import org.amv.access.client.model.DeviceCertificateDto;
import org.amv.access.config.SqliteTestDatabaseConfig;
import org.amv.access.core.Issuer;
import org.amv.access.demo.DemoService;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.util.MoreHex;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
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
import reactor.core.publisher.Mono;

import javax.servlet.ServletContext;
import java.util.Optional;

import static org.apache.commons.codec.binary.Base64.isBase64;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AmvAccessApplication.class, SqliteTestDatabaseConfig.class}
)
public class DeviceCertificateCtrlTestWithRestClient {

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

        CreateDeviceCertificateRequestDto body = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey(publicKeyBase64)
                .build();

        String appId = application.getAppId();
        String nonExistingApiKey = application.getApiKey() + "123";

        try {
            this.deviceCertClient
                    .createDeviceCertificate(appId, nonExistingApiKey, body)
                    .execute();

            Assert.fail("Should have thrown exception.");
        } catch (HystrixRuntimeException e) {
            assertThat(e.getCause(), is(notNullValue()));
            assertThat(e.getCause(), instanceOf(FeignException.class));
            assertThat(((FeignException) e.getCause()).status(), is(HttpStatus.UNAUTHORIZED.value()));
        }
    }

    @Test
    public void itShouldCreateDeviceCertificateSuccessfully() throws Exception {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();
        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        CreateDeviceCertificateRequestDto body = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey(publicKeyBase64)
                .build();

        CreateDeviceCertificateResponseDto response = this.deviceCertClient
                .createDeviceCertificate(application.getAppId(), application.getApiKey(), body)
                .execute();
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

        String issuerNameInHex = deviceCertificate.substring(0, 8);
        String applicationIdInHex = deviceCertificate.substring(8, 32);
        String deviceSerialNumberInHex = deviceCertificate.substring(32, 50);
        String devicePublicKeyInHex = deviceCertificate.substring(50, deviceCertificate.length());

        assertThat(issuerNameInHex, is(issuer.getNameInHex()));
        assertThat(applicationIdInHex, is(application.getAppId()));
        assertThat(MoreHex.isHex(deviceSerialNumberInHex), is(true));
        assertThat(devicePublicKeyInHex, is(equalToIgnoringCase(keys.getPublicKey())));
    }

    @Test
    @Ignore("not implemented yet")
    public void itShouldRevokeDeviceCertificate() throws Exception {
        // TODO: implement me
    }
}
