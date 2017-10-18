package org.amv.access.jetty;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;

import java.lang.management.ManagementFactory;

class JettyJmxCustomizer extends JettyCustomizer {
    @Override
    void customizeJetty(JettyEmbeddedServletContainerFactory container) {
        container.addServerCustomizers(server -> {
            MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
            server.addEventListener(mbContainer);
            server.addBean(mbContainer);
        });
    }
}
