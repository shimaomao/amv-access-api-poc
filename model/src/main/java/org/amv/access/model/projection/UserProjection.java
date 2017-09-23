package org.amv.access.model.projection;

import org.amv.access.model.User;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "defaultUserProjection", types = {User.class})
public interface UserProjection {
    String getName();

    boolean isEnabled();
}
