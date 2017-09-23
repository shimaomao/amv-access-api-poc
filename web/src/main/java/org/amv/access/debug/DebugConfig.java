package org.amv.access.debug;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.ServletContextRequestLoggingFilter;

@Configuration
@Profile("debug")
public class DebugConfig {

    @Bean
    public ServletContextRequestLoggingFilter requestLoggingFilter() {
        ServletContextRequestLoggingFilter loggingFilter = new ServletContextRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludeHeaders(true);

        /** payload will be logged by {@link DebugConfig#payloadLoggingFilter} */
        loggingFilter.setIncludePayload(false);
        return loggingFilter;
    }

    @Bean
    public PayloadLoggingFilter payloadLoggingFilter() {
        return new PayloadLoggingFilter();
    }
}
