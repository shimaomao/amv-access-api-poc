package org.amv.access.demo;

import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.access.model.UserRepository;
import org.amv.access.model.VehicleRepository;
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
                                   UserRepository userRepository,
                                   VehicleRepository vehicleRepository) {
        return new DemoService(
                cryptotool,
                passwordEncoder,
                userRepository,
                vehicleRepository);
    }

    @Bean
    public CommandLineRunner demo(DemoService demoService) {
        return new CommandLineRunner() {
            @Override
            @Transactional
            public void run(String... args) throws Exception {
                demoService.getOrCreateDemoUser();
                demoService.createDemoData();
            }
        };
    }
}
