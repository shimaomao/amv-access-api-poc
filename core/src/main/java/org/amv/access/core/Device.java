package org.amv.access.core;

public interface Device {
    String getName();

    SerialNumber getSerialNumber();

    Key getPublicKey();
}
