package org.amv.access.config;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Slf4jRequestLog;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.management.ManagementFactory;

import static java.util.Objects.requireNonNull;

@Configuration
public class JettyConfiguration {
    @Bean
    public StatisticsHandler statisticsHandler() {
        return new StatisticsHandler();
    }

    @Bean
    public JettyCustomizer jettyMetricsCustomizer(StatisticsHandler statisticsHandler) {
        return new JettyMetricsCustomizer(statisticsHandler);
    }

    @Bean
    public JettyCustomizer jettyAccessLogCustomizer() {
        return new JettyAccessLogCustomizer();
    }

    @Bean
    public JettyCustomizer jettyJmxCustomizer() {
        return new JettyJmxCustomizer();
    }

    public static abstract class JettyCustomizer implements EmbeddedServletContainerCustomizer {
        @Override
        public void customize(ConfigurableEmbeddedServletContainer container) {
            if (container instanceof JettyEmbeddedServletContainerFactory) {
                customizeJetty((JettyEmbeddedServletContainerFactory) container);
            }
        }

        abstract void customizeJetty(JettyEmbeddedServletContainerFactory container);
    }

    public static class JettyJmxCustomizer extends JettyCustomizer {
        @Override
        void customizeJetty(JettyEmbeddedServletContainerFactory container) {
            container.addServerCustomizers(server -> {
                MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
                server.addEventListener(mbContainer);
                server.addBean(mbContainer);
            });
        }
    }

    public static class JettyAccessLogCustomizer extends JettyCustomizer {
        @Override
        void customizeJetty(JettyEmbeddedServletContainerFactory container) {
            container.addServerCustomizers(server -> {
                RequestLogHandler requestLogsHandler = new RequestLogHandler();
                requestLogsHandler.setServer(server);
                Slf4jRequestLog log = new Slf4jRequestLog();
                requestLogsHandler.setRequestLog(log);

                HandlerCollection handlers = new HandlerCollection(server.getHandlers());
                handlers.prependHandler(requestLogsHandler);
                server.setHandler(handlers);
            });
        }
    }

    public static class JettyMetricsCustomizer extends JettyCustomizer {

        private final StatisticsHandler statisticsHandler;

        JettyMetricsCustomizer(StatisticsHandler statisticsHandler) {
            this.statisticsHandler = requireNonNull(statisticsHandler);
        }

        @Override
        void customizeJetty(JettyEmbeddedServletContainerFactory jetty) {
            String serverName = "jetty";
            jetty.setServerHeader(serverName);
            jetty.setDisplayName(serverName);

            addStatisticHandler(jetty);
        }

        private void addStatisticHandler(JettyEmbeddedServletContainerFactory jetty) {
            jetty.addServerCustomizers(server -> {
                HandlerCollection handlerCollection = new HandlerCollection(server.getHandlers());
                statisticsHandler.setHandler(handlerCollection);

                server.setHandler(statisticsHandler);
            });
        }
    }

}
