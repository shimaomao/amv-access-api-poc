package org.amv.access.api.access.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(builderClassName = "Builder")
public class RevokeAccessCertificateRequest {
    @NonNull
    private String deviceSerialNumber;
    @NonNull
    private UUID accessCertificateId;
}
