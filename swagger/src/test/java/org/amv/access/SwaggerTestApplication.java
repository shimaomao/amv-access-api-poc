package org.amv.access;


import org.amv.access.swagger.SwaggerConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@Import(SwaggerConfiguration.class)
public class SwaggerTestApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(SwaggerTestApplication.class)
                .web(true)
                .run(args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        int logRounds = 11;
        return new BCryptPasswordEncoder(logRounds);
    }
}
