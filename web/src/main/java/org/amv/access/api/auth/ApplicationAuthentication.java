package org.amv.access.api.auth;

import org.amv.access.model.Application;

public interface ApplicationAuthentication {
    String getAppId();

    Application getApplication();
}
