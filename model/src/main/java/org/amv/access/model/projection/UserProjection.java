package org.amv.access.model.projection;

import org.amv.access.model.UserEntity;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "defaultUserProjection", types = {UserEntity.class})
public interface UserProjection {
    String getName();

    boolean isEnabled();
}
