package org.amv.access.demo;

import lombok.Builder;
import lombok.Value;
import org.amv.access.model.DeviceEntity;
import org.amv.access.model.IssuerEntity;
import org.amv.highmobility.cryptotool.Cryptotool;

@Value
@Builder
public class IssuerWithKeys {
    private IssuerEntity issuer;
    private Cryptotool.Keys keys;
}
