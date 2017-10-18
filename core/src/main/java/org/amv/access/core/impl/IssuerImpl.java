package org.amv.access.core.impl;


import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.core.Issuer;

@Value
@Builder(builderClassName = "Builder")
public class IssuerImpl implements Issuer {
    @NonNull
    private String name;
    @NonNull
    private String publicKeyBase64;
    @NonNull
    private String privateKeyBase64;
}
