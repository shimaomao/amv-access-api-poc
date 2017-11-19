package org.amv.access.api.internal;

import org.amv.highmobility.cryptotool.Cryptotool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/internal/cryptotool")
public class CryptotoolCtrl {

    private final Cryptotool cryptotool;

    @Autowired
    public CryptotoolCtrl(Cryptotool cryptotool) {
        this.cryptotool = requireNonNull(cryptotool);
    }

    @GetMapping("/version")
    public ResponseEntity<Cryptotool.Version> version() {
        final Cryptotool.Version version = cryptotool.version().block();

        return ResponseEntity.ok(version);
    }

    @PostMapping("/keys")
    public ResponseEntity<Cryptotool.Keys> generateKeys() {
        final Cryptotool.Keys keys = cryptotool.generateKeys().block();

        return ResponseEntity.ok(keys);
    }

    @GetMapping("/signature")
    public ResponseEntity<Cryptotool.Signature> generateSignature(
            @RequestParam("message") String message,
            @RequestParam("private_key") String privateKey) {
        final Cryptotool.Signature signature = cryptotool.generateSignature(message, privateKey).block();

        return ResponseEntity.ok(signature);
    }

    @GetMapping("/signature/{signature}")
    public ResponseEntity<Cryptotool.Validity> verifySignature(
            @PathVariable("signature") String signature,
            @RequestParam("message") String message,
            @RequestParam("public_key") String publicKey) {
        final Cryptotool.Validity validity = cryptotool.verifySignature(message, signature, publicKey).block();

        return ResponseEntity.ok(validity);
    }
}
