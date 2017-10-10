package org.amv.access.auth;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.model.ApplicationEntity;

@Value
@Builder
public class ApplicationAuthenticationImpl implements ApplicationAuthentication {
    @NonNull
    private ApplicationEntity application;

    @Override
    public String getAppId() {
        return application.getAppId();
    }
}
