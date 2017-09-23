package org.amv.access.api.device;

import org.amv.access.model.DeviceCertificateRepository;
import org.amv.access.model.DeviceCertificateRequestRepository;
import org.amv.access.model.DeviceRepository;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceCertificateConfig {

    @Bean
    public CreateDeviceCertificateRequestValidator createDeviceCertificateRequestValidator() {
        return new CreateDeviceCertificateRequestValidator();
    }

    @Bean
    public DeviceCertificateService deviceCertificateService(AmvAccessModuleSpi amvAccessModule,
                                                             DeviceRepository deviceRepository,
                                                             DeviceCertificateRepository deviceCertificateRepository,
                                                             DeviceCertificateRequestRepository deviceCertificateRequestRepository) {
        return new DeviceCertificateServiceImpl(amvAccessModule,
                deviceRepository,
                deviceCertificateRepository,
                deviceCertificateRequestRepository);
    }
}
