package org.amv.access.spi;

import org.amv.access.core.Application;
import org.amv.access.core.Device;

public interface CreateDeviceCertificateRequest {
    /**
     * @return the application requesting a certificate
     */
    Application getApplication();

    /**
     * @return the device the created certificate is for
     */
    Device getDevice();
}
