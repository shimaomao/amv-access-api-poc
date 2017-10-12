package org.amv.access.swagger;

import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.data.rest.configuration.SpringDataRestConfiguration;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Predicates.and;
import static java.util.Objects.requireNonNull;
import static springfox.documentation.builders.RequestHandlerSelectors.withClassAnnotation;

@Configuration
@EnableConfigurationProperties(SwaggerProperties.class)
@ConditionalOnProperty(value = "amv.swagger.enabled", matchIfMissing = true)
@EnableSwagger2
@Import({
        SpringDataRestConfiguration.class
})
public class SwaggerConfiguration extends WebMvcConfigurerAdapter {
    private static final TypeResolver typeResolver = new TypeResolver();

    private SwaggerProperties swaggerProperties;

    @Autowired
    public SwaggerConfiguration(SwaggerProperties swaggerProperties) {
        this.swaggerProperties = requireNonNull(swaggerProperties);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        int cachePeriodInSeconds = (int) TimeUnit.DAYS.toSeconds(1);

        if (!registry.hasMappingForPattern("/webjars/**")) {
            registry.addResourceHandler("/webjars/**")
                    .addResourceLocations("classpath:/META-INF/resources/webjars/")
                    .setCachePeriod(cachePeriodInSeconds);
        }

        registry.addResourceHandler("/swagger.html")
                .addResourceLocations("classpath:/static/swagger-ui.html")
                .setCachePeriod(cachePeriodInSeconds);
    }

    @Bean
    public ApiInfo apiInfo() {
        return toApiInfo(swaggerProperties);
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("api")
                .select()
                .apis(withClassAnnotation(RestController.class))
                .paths(PathSelectors.regex("/api/.+"))
                .build()
                .apiInfo(apiInfo());
    }

    @Bean
    public Docket internal() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("internal")
                .select()
                .apis(withClassAnnotation(RestController.class))
                .paths(PathSelectors.regex("/internal/.+"))
                .build()
                .apiInfo(apiInfo());
    }

    @Bean
    public Docket models() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("models")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.regex("/model-.+"))
                .build()
                .apiInfo(apiInfo());
    }

    @Bean
    public Docket all() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("all")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo());
    }

    // TODO: this is not working correctly with Spring Boot 2.0.0 :/ (2017-10-12)
    private Predicate<RequestHandler> modelApiHandlerPredicate() {
        return and(
                withClassAnnotation(RepositoryRestController.class)
        );
    }

    private ApiInfo toApiInfo(SwaggerProperties properties) {
        String version = Optional.ofNullable(this.getClass())
                .map(Class::getPackage)
                .map(Package::getImplementationVersion)
                .orElse(properties.getVersion());

        return new ApiInfo(
                properties.getTitle(),
                properties.getDescription(),
                version,
                properties.getTermsOfServiceUrl(),
                new Contact(properties.getContactName(),
                        properties.getContactUrl(),
                        properties.getContactEmail()),
                properties.getLicense(),
                properties.getLicenseUrl(),
                Collections.emptyList()
        );
    }
}