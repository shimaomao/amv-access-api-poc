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
    private static void clearDefaultRegistryToPreventScopeRefreshBugDuringTest() {
        // avoids duplicate metrics registration in case of spring boot dev-tools restarts
        CollectorRegistry.defaultRegistry.clear();
    }

    static {
        clearDefaultRegistryToPreventScopeRefreshBugDuringTest();
    }

    public PrometheusConfiguration() {
        DefaultExports.initialize();
    }

    @Bean
    @ConditionalOnMissingBean
    public CollectorRegistry collectorRegistry() {
        CollectorRegistry defaultRegistry = CollectorRegistry.defaultRegistry;
        defaultRegistry.clear();
        return defaultRegistry;
    }

    @Bean
    public CacheMetricsCollector cacheMetricsCollector(CollectorRegistry collectorRegistry) {
        return new CacheMetricsCollector().register(collectorRegistry);
    }
}
