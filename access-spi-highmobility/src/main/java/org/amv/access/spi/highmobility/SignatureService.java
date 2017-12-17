package org.amv.access.spi.highmobility;

import reactor.core.publisher.Mono;

public interface SignatureService {
    Mono<String> generateSignature(String messageBase64, String privateKeyBase64);

    Mono<Boolean> verifySignature(String messageBase64, String signatureBase64, String publicKeyBase64);
}
