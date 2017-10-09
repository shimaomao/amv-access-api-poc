package org.amv.access.auth;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.model.Application;

@Value
@Builder
public class ApplicationAuthenticationImpl implements ApplicationAuthentication {
    @NonNull
    private Application application;

    @Override
    public String getAppId() {
        return application.getAppId();
    }
}
