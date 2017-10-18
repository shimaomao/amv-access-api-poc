package org.amv.access.jetty;

import org.eclipse.jetty.server.Slf4jRequestLog;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;

class JettyAccessLogCustomizer extends JettyCustomizer {
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
