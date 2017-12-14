package org.amv.access.config;

import org.amv.access.spi.highmobility.HighmobilityModule;
import org.amv.highmobility.cryptotool.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

@Configuration
public class HighmobilityConfig {

    @Bean
    public BinaryExecutor binaryExecutor() throws URISyntaxException, IOException {
        return BinaryExecutorImpl.createDefault();
    }

    @Bean
    public CryptotoolOptions cryptotoolOptions(BinaryExecutor binaryExecutor) {
        requireNonNull(binaryExecutor, "`binaryExecutor` must not be null");

        return new CryptotoolOptions() {
            @Override
            public BinaryExecutor getBinaryExecutor() {
                return binaryExecutor;
            }

            @Override
            public Duration getCommandTimeout() {
                return Duration.ofSeconds(10L);
            }
        };
    }

    @Bean
    public Cryptotool cryptotool(CryptotoolOptions cryptotoolOptions) {
        requireNonNull(cryptotoolOptions, "`cryptotoolOptions` must not be null");
        return new CryptotoolImpl(cryptotoolOptions);
    }

    @Bean
    public HighmobilityModule highmobilityModule(Cryptotool cryptotool) {
        return new HighmobilityModule(cryptotool);
    }
}
