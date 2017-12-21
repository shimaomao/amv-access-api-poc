package org.amv.access.api.device;

import org.amv.access.certificate.DeviceCertificateService;
import org.amv.access.issuer.IssuerService;
import org.amv.access.model.ApplicationRepository;
import org.amv.access.model.DeviceCertificateRepository;
import org.amv.access.model.DeviceRepository;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceCertificateConfig {

    @Bean
    public DeviceCertificateService deviceCertificateService(AmvAccessModuleSpi amvAccessModule,
                                                             IssuerService issuerService,
                                                             ApplicationRepository applicationRepository,
                                                             DeviceRepository deviceRepository,
                                                             DeviceCertificateRepository deviceCertificateRepository) {
        return new DeviceCertificateServiceImpl(amvAccessModule,
                issuerService,
                applicationRepository,
                deviceRepository,
                deviceCertificateRepository);
    }
}
