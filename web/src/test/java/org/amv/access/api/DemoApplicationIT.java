package org.amv.access.api;

import org.amv.access.AmvAccessApplication;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.client.AccessCertClient;
import org.amv.access.client.Clients;
import org.amv.access.client.DeviceCertClient;
import org.amv.access.client.model.*;
import org.amv.access.core.Issuer;
import org.amv.access.core.Key;
import org.amv.access.core.impl.KeyImpl;
import org.amv.access.database.EmbeddedMySqlConfig;
import org.amv.access.demo.DemoService;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.spi.highmobility.NonceAuthenticationService;
import org.amv.access.spi.highmobility.NonceAuthenticationServiceImpl;
import org.amv.access.util.MoreHex;
import org.amv.access.util.OperationSystemHelper;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
        classes = {
                AmvAccessApplication.class,
                EmbeddedMySqlConfig.class
        }
)
@ActiveProfiles("embedded-mysql-application-it")
public class DemoApplicationIT {

    @BeforeClass
    public static void skipWindowsOs() {
        Assume.assumeFalse(OperationSystemHelper.isWindows());
    }

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

    private AccessCertClient accessCertClient;

    private ApplicationEntity application;

    private NonceAuthenticationService nonceAuthService;

    @Before
    public void setUp() {
        this.nonceAuthService = new NonceAuthenticationServiceImpl(cryptotool);
        this.application = demoService.getOrCreateDemoApplication();

        String baseUrl = String.format("http://localhost:%d/%s", port, servletContext.getContextPath());
        this.deviceCertClient = Clients.simpleDeviceCertClient(baseUrl);
        this.accessCertClient = Clients.simpleAccessCertClient(baseUrl);
    }

    @Test
    public void itShouldCreateDeviceCertificateSuccessfully() throws Exception {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        Key devicePublicKey = KeyImpl.fromHex(keys.getPublicKey());
        Key devicePrivateKey = KeyImpl.fromHex(keys.getPrivateKey());

        CreateDeviceCertificateRequestDto body = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey(devicePublicKey.toBase64())
                .build();

        // ---- create device certificate
        CreateDeviceCertificateResponseDto deviceCertResponse = this.deviceCertClient
                .createDeviceCertificate(application.getAppId().toHex(), application.getApiKey(), body)
                .execute();

        // ---- validate device certificate
        assertThat(deviceCertResponse, is(notNullValue()));

        DeviceCertificateDto responseDeviceCertificate = deviceCertResponse.getDeviceCertificate();
        assertThat(responseDeviceCertificate, is(notNullValue()));

        String expectedIssuerPublicKey = issuer.getPublicKey().toBase64();
        assertThat(responseDeviceCertificate.getIssuerPublicKey(), is(expectedIssuerPublicKey));

        String signedDeviceCertificate = responseDeviceCertificate.getDeviceCertificate();
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

        String deviceSerialNumberInHex = deviceCertificate.substring(32, 50);
        assertThat(MoreHex.isHex(deviceSerialNumberInHex), is(true));

        // ---- fetch access certificate
        NonceAuthentication nonceAuthentication = nonceAuthService.createNonceAuthentication(devicePrivateKey);

        GetAccessCertificatesResponseDto accessCertResponse = this.accessCertClient
                .fetchAccessCertificates(nonceAuthentication.getNonceBase64(), nonceAuthentication.getNonceSignatureBase64(), deviceSerialNumberInHex)
                .execute();

        List<AccessCertificateDto> accessCertificates = accessCertResponse.getAccessCertificates();
        assertThat(accessCertificates, hasSize(1));
    }
}
