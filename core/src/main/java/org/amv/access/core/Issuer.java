package org.amv.access.core;

import com.google.common.base.Charsets;
import org.apache.commons.codec.binary.Hex;

import java.util.Optional;

public interface Issuer {
    String getName();

    Key getPublicKey();

    Optional<Key> getPrivateKey();

    default String getNameInHex() {
        return Optional.ofNullable(getName())
                .map(val -> val.getBytes(Charsets.UTF_8))
                .map(Hex::encodeHexString)
                .orElseThrow(IllegalStateException::new);
    }
}
