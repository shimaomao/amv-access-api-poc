package org.amv.access.test;

import lombok.Builder;
import lombok.Value;
import org.amv.access.model.Device;
import org.amv.highmobility.cryptotool.Cryptotool;

@Value
@Builder
public class DeviceWithKeys {
    private Device device;
    private Cryptotool.Keys keys;
}
