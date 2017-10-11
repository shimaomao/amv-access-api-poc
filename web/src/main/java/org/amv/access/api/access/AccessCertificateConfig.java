package org.amv.access.api.access;

import org.amv.access.model.*;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessCertificateConfig {

    @Bean
    public CreateAccessCertificateRequestValidator createAccessCertificateRequestValidator() {
        return new CreateAccessCertificateRequestValidator();
    }

    @Bean
    public AccessCertificateService accessCertificateService(AmvAccessModuleSpi amvAccessModule,
                                                             IssuerRepository issuerRepository,
                                                             ApplicationRepository applicationRepository,
                                                             VehicleRepository vehicleRepository,
                                                             DeviceRepository deviceRepository,
                                                             AccessCertificateRepository accessCertificateRepository,
                                                             AccessCertificateRequestRepository accessCertificateRequestRepository) {
        return new AccessCertificateServiceImpl(amvAccessModule,
                issuerRepository,
                applicationRepository,
                vehicleRepository,
                deviceRepository,
                accessCertificateRepository,
                accessCertificateRequestRepository);
    }
}
