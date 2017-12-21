package org.amv.access.spi.highmobility;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.auth.NonceAuthenticationImpl;
import org.amv.access.core.Key;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.apache.commons.lang3.RandomUtils;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Slf4j
public class NonceAuthenticationServiceImpl implements NonceAuthenticationService {

    private final Cryptotool cryptotool;

    public NonceAuthenticationServiceImpl(Cryptotool cryptotool) {
        this.cryptotool = requireNonNull(cryptotool);
    }

    @Override
    public NonceAuthentication createNonceAuthentication(Key privateKey) {
        String nonceBase64 = createNonceWithRandomLengthBase64();
        String nonceSignatureBase64 = createNonceSignatureBase64(privateKey, nonceBase64);

        return NonceAuthenticationImpl.builder()
                .nonceBase64(nonceBase64)
                .nonceSignatureBase64(nonceSignatureBase64)
                .build();
    }

    @Override
    public NonceAuthentication createAndVerifyNonceAuthentication(Key privateKey, Key publicKey) {
        NonceAuthentication auth = createNonceAuthentication(privateKey);

        String nonceInHex = CryptotoolUtils.decodeBase64AsHex(auth.getNonceBase64());
        String signatureInHex = CryptotoolUtils.decodeBase64AsHex(auth.getNonceSignatureBase64());

        verifySignatureOrThrow(publicKey, nonceInHex, signatureInHex);

        return auth;
    }

    private String createNonceWithRandomLengthBase64() {
        return createNonceBase64(RandomUtils.nextInt(64, 128));
    }

    private String createNonceBase64(int numberOfBytes) {
        return CryptotoolUtils.SecureRandomUtils.generateRandomHexString(numberOfBytes);
    }

    private String createNonceSignatureBase64(Key privateKey, String nonceBase64) {
        String nonceInHex = CryptotoolUtils.decodeBase64AsHex(nonceBase64);
        return Optional.of(cryptotool.generateSignature(nonceInHex, privateKey.toHex()))
                .map(Mono::block)
                .map(Cryptotool.Signature::getSignature)
                .map(CryptotoolUtils::encodeHexAsBase64)
                .orElseThrow(IllegalStateException::new);
    }

    private void verifySignatureOrThrow(Key publicKey, String nonceInHex, String signatureInHex) {
        Cryptotool.Validity signedNonceValidity = Optional.of(cryptotool.verifySignature(
                nonceInHex,
                signatureInHex,
                publicKey.toHex()))
                .map(Mono::block)
                .orElse(Cryptotool.Validity.INVALID);

        if (log.isDebugEnabled()) {
            log.debug("Create nonce authentication:\n" +
                            "validity: {}\n" +
                            "nonce: {}\n" +
                            "nonce-signature: {}\n" +
                            "public key: {}\n",
                    signedNonceValidity,
                    nonceInHex,
                    signatureInHex,
                    publicKey.toHex());
        }

        if (signedNonceValidity == Cryptotool.Validity.INVALID) {
            throw new IllegalStateException("Could not create valid nonce auth object");
        }
    }
}
