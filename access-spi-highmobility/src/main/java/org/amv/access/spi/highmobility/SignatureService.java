package org.amv.access.spi.highmobility;

import org.amv.access.core.Key;
import reactor.core.publisher.Mono;

public interface SignatureService {
    Mono<String> generateSignature(String messageBase64, Key privateKey);

    Mono<Boolean> verifySignature(String messageBase64, String signature, Key publicKey);
}
