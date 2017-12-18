package org.amv.access.config;

import org.amv.access.spi.highmobility.HighmobilityModule;
import org.amv.access.spi.highmobility.SignatureService;
import org.amv.access.spi.highmobility.SignatureServiceImpl;
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
    public HighmobilityModule highmobilityModule(Cryptotool cryptotool, SignatureService signatureService) {
        return new HighmobilityModule(cryptotool, signatureService);
    }

    @Bean
    public SignatureService signatureServiceImpl(Cryptotool cryptotool) {
        return new SignatureServiceImpl(cryptotool);
    }

    @Bean
    public Cryptotool cryptotool(CryptotoolOptions cryptotoolOptions) {
        requireNonNull(cryptotoolOptions, "`cryptotoolOptions` must not be null");
        return new CryptotoolImpl(cryptotoolOptions);
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
                return Duration.ofSeconds(15L);
            }
        };
    }

    @Bean
    public BinaryExecutor binaryExecutor() throws URISyntaxException, IOException {
        return BinaryExecutorImpl.createDefault();
    }

}
