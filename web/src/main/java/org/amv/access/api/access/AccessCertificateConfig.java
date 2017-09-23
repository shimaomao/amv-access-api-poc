package org.amv.access.api.access;

import org.amv.access.model.AccessCertificateRepository;
import org.amv.access.model.AccessCertificateRequestRepository;
import org.amv.access.model.DeviceRepository;
import org.amv.access.model.VehicleRepository;
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
                                                             VehicleRepository vehicleRepository,
                                                             DeviceRepository deviceRepository,
                                                             AccessCertificateRepository accessCertificateRepository,
                                                             AccessCertificateRequestRepository accessCertificateRequestRepository) {
        return new AccessCertificateServiceImpl(amvAccessModule,
                vehicleRepository,
                deviceRepository,
                accessCertificateRepository,
                accessCertificateRequestRepository);
    }
}
