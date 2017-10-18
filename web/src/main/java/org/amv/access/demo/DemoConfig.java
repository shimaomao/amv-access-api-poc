package org.amv.access.demo;

import org.amv.access.model.*;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DemoConfig {

    @Bean
    public DemoService demoService(Cryptotool cryptotool,
                                   PasswordEncoder passwordEncoder,
                                   IssuerRepository issuerRepository,
                                   ApplicationRepository applicationRepository,
                                   UserRepository userRepository,
                                   VehicleRepository vehicleRepository,
                                   DeviceRepository deviceRepository) {
        return new DemoService(
                cryptotool,
                passwordEncoder,
                issuerRepository,
                applicationRepository,
                userRepository,
                vehicleRepository,
                deviceRepository);
    }

    @Bean
    public InitializingBean demo(DemoService demoService) {
        return new InitializingBean() {
            @Override
            @Transactional
            public void afterPropertiesSet() throws Exception {
                demoService.createDemoData();
            }
        };
    }
}
