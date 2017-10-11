package org.amv.access.demo;

import org.amv.access.model.ApplicationRepository;
import org.amv.access.model.DeviceRepository;
import org.amv.access.model.UserRepository;
import org.amv.access.model.VehicleRepository;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DemoConfig {

    @Bean
    public DemoService demoService(Cryptotool cryptotool,
                                   PasswordEncoder passwordEncoder,
                                   ApplicationRepository applicationRepository,
                                   UserRepository userRepository,
                                   VehicleRepository vehicleRepository,
                                   DeviceRepository deviceRepository) {
        return new DemoService(
                cryptotool,
                passwordEncoder,
                applicationRepository,
                userRepository,
                vehicleRepository,
                deviceRepository);
    }

    @Bean
    public CommandLineRunner demo(DemoService demoService) {
        return new CommandLineRunner() {
            @Override
            @Transactional
            public void run(String... args) throws Exception {
                demoService.createDemoData();
            }
        };
    }
}
