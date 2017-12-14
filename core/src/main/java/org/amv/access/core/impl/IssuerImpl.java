package org.amv.access.core.impl;


import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.core.Issuer;

import java.util.Optional;

@Value
@Builder(builderClassName = "Builder")
public class IssuerImpl implements Issuer {
    @NonNull
    private String name;
    @NonNull
    private String publicKeyBase64;

    private String privateKeyBase64;

    @Override
    public Optional<String> getPrivateKeyBase64() {
        return Optional.ofNullable(privateKeyBase64);
    }
}
