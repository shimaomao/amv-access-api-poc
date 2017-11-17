package org.amv.access.api.access;

import org.amv.access.issuer.IssuerService;
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
                                                             IssuerService issuerService,
                                                             ApplicationRepository applicationRepository,
                                                             VehicleRepository vehicleRepository,
                                                             DeviceRepository deviceRepository,
                                                             DeviceCertificateRepository deviceCertificateRepository,
                                                             AccessCertificateRepository accessCertificateRepository) {
        return new AccessCertificateServiceImpl(amvAccessModule,
                issuerService,
                applicationRepository,
                vehicleRepository,
                deviceRepository,
                deviceCertificateRepository,
                accessCertificateRepository);
    }
}
