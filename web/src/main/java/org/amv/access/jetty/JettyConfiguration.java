package org.amv.access.jetty;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.jetty.JettyStatisticsCollector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JettyConfiguration {
    @Bean
    public StatisticsHandler statisticsHandler() {
        return new StatisticsHandler();
    }

    @Bean
    public JettyStatisticsCollector jettyStatisticsCollector(CollectorRegistry metricRegistry,
                                                             StatisticsHandler statisticsHandler) {
        return new JettyStatisticsCollector(statisticsHandler)
                .register(metricRegistry);
    }

    @Bean
    public JettyCustomizer jettyCustomizer() {
        return new JettyCustomizer() {
            @Override
            void customizeJetty(JettyEmbeddedServletContainerFactory container) {
                String serverName = "jetty";
                container.setServerHeader(serverName);
                container.setDisplayName(serverName);
                container.setUseForwardHeaders(true);
            }
        };
    }

    @Bean
    public JettyCustomizer jettyAccessLogCustomizer() {
        return new JettyAccessLogCustomizer();
    }

    @Bean
    public JettyCustomizer jettyJmxCustomizer() {
        return new JettyJmxCustomizer();
    }

    @Bean
    public JettyCustomizer jettyMetricsCustomizer(StatisticsHandler statisticsHandler) {
        return new JettyMetricsCustomizer(statisticsHandler);
    }
}
