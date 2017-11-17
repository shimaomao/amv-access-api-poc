package org.amv.access.demo;

import io.vertx.rxjava.core.eventbus.EventBus;
import org.amv.access.api.access.AccessCertificateService;
import org.amv.access.api.device.DeviceCertificateService;
import org.amv.access.model.*;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.requireNonNull;

@Configuration
@EnableConfigurationProperties(DemoProperties.class)
public class DemoConfig {

    private final DemoProperties demoProperties;

    @Autowired
    public DemoConfig(DemoProperties demoProperties) {
        this.demoProperties = requireNonNull(demoProperties);
    }

    @Bean
    public DemoService demoService(Cryptotool cryptotool,
                                   PasswordEncoder passwordEncoder,
                                   IssuerRepository issuerRepository,
                                   ApplicationRepository applicationRepository,
                                   UserRepository userRepository,
                                   VehicleRepository vehicleRepository,
                                   DeviceRepository deviceRepository,
                                   DeviceCertificateService deviceCertificateService) {
        return new DemoServiceImpl(
                cryptotool,
                passwordEncoder,
                issuerRepository,
                applicationRepository,
                userRepository,
                vehicleRepository,
                deviceRepository,
                deviceCertificateService);
    }

    @Bean
    public InitializingBean demo(DemoService demoService) {
        return new InitializingBean() {
            @Override
            @Transactional
            public void afterPropertiesSet() throws Exception {
                if (demoProperties.isEnabled()) {
                    demoService.createDemoDataFromProperties(demoProperties);
                }
            }
        };
    }


    @Bean
    public DemoAccessCertificateVerticle demoAccessCertificateVerticle(EventBus eventBus,
                                                                       DemoService demoService,
                                                                       DeviceRepository deviceRepository,
                                                                       AccessCertificateService accessCertificateService) {
        return new DemoAccessCertificateVerticle(
                eventBus,
                demoService,
                deviceRepository,
                accessCertificateService);
    }
}
