package org.amv.access.auth;

import org.amv.access.model.Application;

public interface ApplicationAuthentication {
    String getAppId();

    Application getApplication();
}
