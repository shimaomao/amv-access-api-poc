package org.amv.access.api.device;

import io.vertx.rxjava.core.eventbus.EventBus;
import org.amv.access.issuer.IssuerService;
import org.amv.access.model.ApplicationRepository;
import org.amv.access.model.DeviceCertificateRepository;
import org.amv.access.model.DeviceCertificateRequestRepository;
import org.amv.access.model.DeviceRepository;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceCertificateConfig {

    @Bean
    public DeviceCertificateService deviceCertificateService(AmvAccessModuleSpi amvAccessModule,
                                                             EventBus eventBus,
                                                             IssuerService issuerService,
                                                             ApplicationRepository applicationRepository,
                                                             DeviceRepository deviceRepository,
                                                             DeviceCertificateRepository deviceCertificateRepository,
                                                             DeviceCertificateRequestRepository deviceCertificateRequestRepository) {
        return new DeviceCertificateServiceImpl(amvAccessModule,
                eventBus,
                issuerService,
                applicationRepository,
                deviceRepository,
                deviceCertificateRepository,
                deviceCertificateRequestRepository);
    }
}
