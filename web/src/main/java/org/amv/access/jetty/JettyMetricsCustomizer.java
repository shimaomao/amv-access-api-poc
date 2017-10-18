package org.amv.access.jetty;

import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;

import static java.util.Objects.requireNonNull;

class JettyMetricsCustomizer extends JettyCustomizer {

    private final StatisticsHandler statisticsHandler;

    JettyMetricsCustomizer(StatisticsHandler statisticsHandler) {
        this.statisticsHandler = requireNonNull(statisticsHandler);
    }

    @Override
    void customizeJetty(JettyEmbeddedServletContainerFactory jetty) {
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
