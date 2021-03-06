package org.amv.access.issuer;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.core.Issuer;
import org.amv.access.core.Key;
import org.amv.access.core.impl.IssuerImpl;
import org.amv.access.core.impl.KeyImpl;
import org.amv.access.model.IssuerEntity;
import org.amv.access.model.IssuerRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

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

        return IssuerImpl.builder()
                .name(name)
                .publicKey(KeyImpl.fromHex(certificateIssuerProperties.getPublicKey()))
                .privateKey(KeyImpl.fromHex(certificateIssuerProperties.getPrivateKey()))
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

        Optional<IssuerEntity> issuerOrEmpty = issuerRepository
                .findByNameAndPublicKeyBase64(issuer.getName(), issuer.getPublicKey().toBase64());

        if (issuerOrEmpty.isPresent()) {
            log.info("Issuer '{}' is already present - nothing to do", issuer.getName());
        } else {
            log.info("Issuer '{}' is not present and will be created", issuer.getName());

            issuerRepository.save(IssuerEntity.builder()
                    .name(issuer.getName())
                    .uuid(UUID.randomUUID().toString())
                    .createdAt(Date.from(Instant.now()))
                    .publicKeyBase64(issuer.getPublicKey().toBase64())
                    .privateKeyBase64(issuer.getPrivateKey().map(Key::toBase64).orElse(null))
                    .build());

            log.info("Issuer '{}' created successfully", issuer.getName());
        }
    }
}
