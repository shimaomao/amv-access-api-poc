package org.amv.access.spi.highmobility;

import lombok.Builder;
import lombok.Value;
import org.amv.access.core.Permissions;
import org.amv.highmobility.cryptotool.Cryptotool;

@Value
@Builder(builderClassName = "Builder")
public class AmvPermissionsAdapter implements Permissions {

    private Cryptotool.Permissions cryptotoolPermissions;
    
    @Override
    public String getPermissions() {
        return cryptotoolPermissions.getPermissions();
    }
}
