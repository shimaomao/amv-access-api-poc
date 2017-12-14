package org.amv.access.auth;

public interface DeviceNonceAuthentication extends NonceAuthentication {
    String getDeviceSerialNumber();
}
