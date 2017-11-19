package org.amv.access.demo;

import lombok.Builder;
import lombok.Value;
import org.amv.access.model.DeviceEntity;
import org.amv.highmobility.cryptotool.Cryptotool;

@Value
@Builder
public class DeviceWithKeys {
    private DeviceEntity device;
    private Cryptotool.Keys keys;
}
