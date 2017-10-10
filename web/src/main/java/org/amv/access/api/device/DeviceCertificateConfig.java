package org.amv.access.api.device;

import org.amv.access.model.*;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceCertificateConfig {

    @Bean
    public DeviceCertificateService deviceCertificateService(AmvAccessModuleSpi amvAccessModule,
                                                             IssuerRepository issuerRepository,
                                                             ApplicationRepository applicationRepository,
                                                             DeviceRepository deviceRepository,
                                                             DeviceCertificateRepository deviceCertificateRepository,
                                                             DeviceCertificateRequestRepository deviceCertificateRequestRepository) {
        return new DeviceCertificateServiceImpl(amvAccessModule,
                issuerRepository,
                applicationRepository,
                deviceRepository,
                deviceCertificateRepository,
                deviceCertificateRequestRepository);
    }
}
