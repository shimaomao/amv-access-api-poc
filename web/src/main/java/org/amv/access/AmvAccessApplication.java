package org.amv.access;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.system.ApplicationPidFileWriter;
import org.springframework.boot.system.EmbeddedServerPortFileWriter;
import org.springframework.context.ApplicationListener;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

@EntityScan(
        basePackageClasses = {
                AmvAccessApplication.class,
                Jsr310JpaConverters.class
        }
)
@SpringBootApplication
public class AmvAccessApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(AmvAccessApplication.class)
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
