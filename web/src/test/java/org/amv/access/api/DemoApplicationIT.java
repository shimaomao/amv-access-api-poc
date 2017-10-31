package org.amv.access.api;

import org.amv.access.AmvAccessApplication;
import org.amv.access.client.AccessCertClient;
import org.amv.access.client.Clients;
import org.amv.access.client.DeviceCertClient;
import org.amv.access.client.model.*;
import org.amv.access.config.TestDbConfig;
import org.amv.access.core.Issuer;
import org.amv.access.demo.DemoService;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.util.MoreBase64;
import org.amv.access.util.MoreHex;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
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
        classes = {AmvAccessApplication.class, TestDbConfig.class}
)
public class DemoApplicationIT {

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

    @Before
    public void setUp() {
        this.application = demoService.getOrCreateDemoApplication();

        String baseUrl = String.format("http://localhost:%d/%s", port, servletContext.getContextPath());
        this.deviceCertClient = Clients.simpleDeviceCertClient(baseUrl);
        this.accessCertClient = Clients.simpleAccessCertClient(baseUrl);
    }

    @Test
    public void itShouldCreateDeviceCertificateSuccessfully() throws Exception {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();
        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        CreateDeviceCertificateRequestDto body = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey(publicKeyBase64)
                .build();

        // ---- create device certificate
        CreateDeviceCertificateResponseDto deviceCertResponse = this.deviceCertClient
                .createDeviceCertificate(application.getApiKey(), body)
                .execute();

        // ---- validate device certificate
        assertThat(deviceCertResponse, is(notNullValue()));

        DeviceCertificateDto responseDeviceCertificate = deviceCertResponse.getDeviceCertificate();
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

        String deviceSerialNumberInHex = deviceCertificate.substring(32, 50);
        assertThat(MoreHex.isHex(deviceSerialNumberInHex), is(true));

        // ---- fetch access certificate
        String nonceBase64 = generateNonceWithRandomLengthBase64();
        String nonceSignatureBase64 = createNonceSignatureBase64(keys, CryptotoolUtils.decodeBase64AsHex(nonceBase64));

        GetAccessCertificatesResponseDto accessCertResponse = this.accessCertClient
                .fetchAccessCertificates(nonceBase64, nonceSignatureBase64, deviceSerialNumberInHex)
                .execute();

        List<AccessCertificateDto> accessCertificates = accessCertResponse.getAccessCertificates();
        assertThat(accessCertificates, hasSize(1));
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
