package org.amv.access.auth;

import org.amv.access.model.ApplicationEntity;

public interface ApplicationAuthentication {
    String getAppId();

    ApplicationEntity getApplication();
}
