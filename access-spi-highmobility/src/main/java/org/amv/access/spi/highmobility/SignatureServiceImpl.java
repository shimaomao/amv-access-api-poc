package org.amv.access.spi.highmobility;

import org.amv.access.core.Key;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import reactor.core.publisher.Mono;

import static java.util.Objects.requireNonNull;
import static org.amv.highmobility.cryptotool.CryptotoolUtils.decodeBase64AsHex;

public class SignatureServiceImpl implements SignatureService {

    private final Cryptotool cryptotool;

    public SignatureServiceImpl(Cryptotool cryptotool) {
        this.cryptotool = requireNonNull(cryptotool);
    }

    @Override
    public Mono<String> generateSignature(String messageBase64, Key privateKey) {
        String messageInHex = decodeBase64AsHex(messageBase64);

        return cryptotool.generateSignature(messageInHex, privateKey.toHex())
                .map(Cryptotool.Signature::getSignature)
                .map(CryptotoolUtils::encodeHexAsBase64);
    }

    @Override
    public Mono<Boolean> verifySignature(String messageBase64, String signatureBase64, Key publicKey) {
        String messageInHex = decodeBase64AsHex(messageBase64);
        String signatureInHex = decodeBase64AsHex(signatureBase64);

        return cryptotool.verifySignature(messageInHex, signatureInHex, publicKey.toHex())
                .map(s -> s == Cryptotool.Validity.VALID);
    }
}
