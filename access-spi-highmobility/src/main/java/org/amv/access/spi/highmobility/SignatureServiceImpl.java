package org.amv.access.spi.highmobility;

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
    public Mono<String> generateSignature(String messageBase64, String privateKeyBase64) {
        String messageInHex = decodeBase64AsHex(messageBase64);
        String issuerPrivateKeyInHex = decodeBase64AsHex(privateKeyBase64);

        return cryptotool.generateSignature(messageInHex, issuerPrivateKeyInHex)
                .map(Cryptotool.Signature::getSignature)
                .map(CryptotoolUtils::encodeHexAsBase64);
    }

    @Override
    public Mono<Boolean> verifySignature(String messageBase64, String signatureBase64, String publicKeyBase64) {
        String messageInHex = decodeBase64AsHex(messageBase64);
        String signatureInHex = decodeBase64AsHex(signatureBase64);
        String publicKeyInHex = decodeBase64AsHex(publicKeyBase64);

        return cryptotool.verifySignature(messageInHex, signatureInHex, publicKeyInHex)
                .map(s -> s == Cryptotool.Validity.VALID);
    }
}
