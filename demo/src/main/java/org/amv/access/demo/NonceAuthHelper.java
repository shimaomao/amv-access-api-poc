package org.amv.access.demo;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.auth.NonceAuthenticationImpl;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.apache.commons.lang3.RandomUtils;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Slf4j
public class NonceAuthHelper {

    private Cryptotool cryptotool;

    public NonceAuthHelper(Cryptotool cryptotool) {
        this.cryptotool = requireNonNull(cryptotool);
    }

    public NonceAuthentication createNonceAuthentication(Cryptotool.Keys keys) {
        String nonceBase64 = createNonceWithRandomLengthBase64();
        String nonceSignatureBase64 = createNonceSignatureBase64(keys, nonceBase64);

        String publicKey = keys.getPublicKey();

        String nonce = CryptotoolUtils.decodeBase64AsHex(nonceBase64);
        Cryptotool.Validity signedNonceValidity = Optional.of(cryptotool.verifySignature(
                nonce,
                CryptotoolUtils.decodeBase64AsHex(nonceSignatureBase64),
                publicKey))
                .map(Mono::block)
                .orElse(Cryptotool.Validity.INVALID);

        if (log.isDebugEnabled()) {
            log.debug("Create nonce authentication:\n" +
                            "validity: {}\n" +
                            "nonce: {}\n" +
                            "nonce-signature: {}\n" +
                            "public key: {}\n",
                    signedNonceValidity,
                    nonceBase64,
                    nonceSignatureBase64,
                    keys.getPublicKey());
        }

        if (signedNonceValidity == Cryptotool.Validity.INVALID) {
            throw new IllegalStateException("Could not create valid nonce auth object");
        }

        return NonceAuthenticationImpl.builder()
                .nonceBase64(nonceBase64)
                .nonceSignatureBase64(nonceSignatureBase64)
                .build();
    }

    public String createNonceWithRandomLengthBase64() {
        return createNonceBase64(RandomUtils.nextInt(64, 128));
    }

    private String createNonceBase64(int numberOfBytes) {
        return CryptotoolUtils.SecureRandomUtils.generateRandomHexString(numberOfBytes);
    }

    public String createNonceSignatureBase64(Cryptotool.Keys keys, String nonceBase64) {
        String nonceInHex = CryptotoolUtils.decodeBase64AsHex(nonceBase64);
        return Optional.of(cryptotool.generateSignature(nonceInHex, keys.getPrivateKey()))
                .map(Mono::block)
                .map(Cryptotool.Signature::getSignature)
                .map(CryptotoolUtils::encodeHexAsBase64)
                .orElseThrow(IllegalStateException::new);
    }
}
