package org.amv.access.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(CollectorRegistry.class)
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector
public class PrometheusConfiguration {

    public PrometheusConfiguration() {
        DefaultExports.initialize();
    }

    @Bean
    @ConditionalOnMissingBean
    public CollectorRegistry metricRegistry() {
        return CollectorRegistry.defaultRegistry;
    }

    @Bean
    public CacheMetricsCollector cacheMetricsCollector(CollectorRegistry metricRegistry) {
        return new CacheMetricsCollector().register(metricRegistry);
    }
}
