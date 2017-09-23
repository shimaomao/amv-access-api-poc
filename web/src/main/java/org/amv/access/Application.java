package org.amv.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.system.ApplicationPidFileWriter;
import org.springframework.boot.system.EmbeddedServerPortFileWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import javax.sql.DataSource;
import java.util.Arrays;

@EntityScan(
        basePackageClasses = {
                Application.class,
                Jsr310JpaConverters.class
        }
)
@SpringBootApplication
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    public static final PageRequest standardPageRequest = new PageRequest(0, 10);

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(Application.class)
                .listeners(applicationPidFileWriter(), embeddedServerPortFileWriter())
                .web(true)
                .run(args);
    }

    public static ApplicationListener<?> applicationPidFileWriter() {
        return new ApplicationPidFileWriter("app.pid");
    }

    public static ApplicationListener<?> embeddedServerPortFileWriter() {
        return new EmbeddedServerPortFileWriter("app.port");
    }
}
