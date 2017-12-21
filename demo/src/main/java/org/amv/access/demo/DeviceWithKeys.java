package org.amv.access.demo;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.core.Key;
import org.amv.access.model.DeviceEntity;
import org.amv.highmobility.cryptotool.Cryptotool;

@Value
@Builder
public class DeviceWithKeys {
    @NonNull
    private DeviceEntity device;
    @NonNull
    private Key publicKey;
    @NonNull
    private Key privateKey;
}
