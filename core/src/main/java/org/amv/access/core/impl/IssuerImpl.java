package org.amv.access.core.impl;


import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.core.Issuer;
import org.amv.access.core.Key;

import java.util.Optional;

@Value
@Builder(builderClassName = "Builder")
public class IssuerImpl implements Issuer {
    @NonNull
    private String name;
    @NonNull
    private Key publicKey;

    private Key privateKey;

    @Override
    public Optional<Key> getPrivateKey() {
        return Optional.ofNullable(privateKey);
    }
}
