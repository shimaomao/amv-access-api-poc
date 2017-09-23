package org.amv.access.spi.highmobility;

import org.amv.highmobility.cryptotool.*;
import org.amv.highmobility.cryptotool.CryptotoolWithIssuer.CertificateIssuer;
import org.amv.highmobility.cryptotool.CryptotoolWithIssuer.CertificateIssuerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

@Configuration
@EnableConfigurationProperties({
        CertificateIssuerProperties.class
})
public class HighmobilityConfig {

    private final CertificateIssuerProperties certificateIssuerProperties;

    @Autowired
    public HighmobilityConfig(CertificateIssuerProperties certificateIssuerProperties) {
        this.certificateIssuerProperties = requireNonNull(certificateIssuerProperties);
    }

    @Bean
    public BinaryExecutor binaryExecutor() throws URISyntaxException, IOException {
        return BinaryExecutorImpl.createDefault();
    }

    @Bean
    public CryptotoolOptions cryptotoolOptions(BinaryExecutor binaryExecutor) {
        requireNonNull(binaryExecutor, "`binaryExecutor` must not be null");

        return new CryptotoolOptions() {
            @Override
            public BinaryExecutor getBinaryExecutor() {
                return binaryExecutor;
            }

            @Override
            public Duration getCommandTimeout() {
                return Duration.ofSeconds(10L);
            }
        };
    }

    @Bean
    public CryptotoolWithIssuer cryptotoolWithIssuer(CryptotoolOptions cryptotoolOptions, CertificateIssuer issuer) {
        requireNonNull(cryptotoolOptions, "`cryptotoolOptions` must not be null");
        requireNonNull(issuer, "`issuer` must not be null");

        return new CryptotoolWithIssuerImpl(cryptotoolOptions, issuer);
    }

    @Bean
    public CertificateIssuer certificateIssuer(Cryptotool.Keys certificateIssuerKeys) throws IOException {
        requireNonNull(certificateIssuerKeys, "`certificateIssuerKeys` must not be null");

        return CertificateIssuerImpl.builder()
                .name(certificateIssuerProperties.getName())
                .keys(certificateIssuerKeys)
                .build();
    }

    @Bean
    public Cryptotool.Keys certificateIssuerKeys() throws IOException {
        return CryptotoolImpl.KeysImpl.builder()
                .privateKey(certificateIssuerProperties.getPrivateKey())
                .publicKey(certificateIssuerProperties.getPublicKey())
                .build();
    }

    @Bean
    public HighmobilityModule highmobilityModule(CryptotoolWithIssuer cryptotoolWithIssuer) {
        return new HighmobilityModule(cryptotoolWithIssuer);
    }
}
