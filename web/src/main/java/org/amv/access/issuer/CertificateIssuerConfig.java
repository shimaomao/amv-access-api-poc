package org.amv.access.issuer;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.core.Issuer;
import org.amv.access.core.impl.IssuerImpl;
import org.amv.access.model.IssuerEntity;
import org.amv.access.model.IssuerRepository;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Slf4j
@Configuration
@EnableConfigurationProperties({
        CertificateIssuerProperties.class
})
public class CertificateIssuerConfig {
    private final CertificateIssuerProperties certificateIssuerProperties;

    @Autowired
    public CertificateIssuerConfig(CertificateIssuerProperties certificateIssuerProperties) {
        this.certificateIssuerProperties = requireNonNull(certificateIssuerProperties);
    }

    @Bean
    public Issuer issuer() {
        String name = certificateIssuerProperties.getName();
        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(certificateIssuerProperties.getPublicKey());
        String privateKeyBase64 = CryptotoolUtils.encodeHexAsBase64(certificateIssuerProperties.getPrivateKey());

        return IssuerImpl.builder()
                .name(name)
                .publicKeyBase64(publicKeyBase64)
                .privateKeyBase64(privateKeyBase64)
                .build();
    }

    @Bean
    public IssuerService issuerService(IssuerRepository issuerRepository) {
        return new IssuerServiceImpl(issuerRepository);
    }

    @Bean
    public InitializingBean issuerInitializer(IssuerRepository issuerRepository) {
        return new InitializingBean() {
            @Override
            @Transactional
            public void afterPropertiesSet() throws Exception {
                initializeIssuerFromPropertiesIfNecessary(issuerRepository, issuer());
            }
        };
    }


    private void initializeIssuerFromPropertiesIfNecessary(IssuerRepository issuerRepository, Issuer issuer) {
        log.info("Creating issuer from properties file: {}", issuer.getName());

        Optional<IssuerEntity> issuerOptional = issuerRepository.findByNameAndPublicKeyBase64(issuer.getName(), issuer.getPublicKeyBase64());
        if (issuerOptional.isPresent()) {
            log.info("Issuer '{}' is already present - nothing to do", issuer.getName());
        } else {
            log.info("Issuer '{}' is not present and will be created", issuer.getName());

            issuerRepository.save(IssuerEntity.builder()
                    .name(issuer.getName())
                    .created(Date.from(Instant.now()))
                    .publicKeyBase64(issuer.getPublicKeyBase64())
                    .privateKeyBase64(issuer.getPrivateKeyBase64())
                    .build());

            log.info("Issuer '{}' created successfully", issuer.getName());
        }
    }
}
