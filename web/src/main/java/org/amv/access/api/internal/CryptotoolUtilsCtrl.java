package org.amv.access.api.internal;

import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/internal/cryptotool/util")
public class CryptotoolUtilsCtrl {

    @PostMapping("/app_id")
    public ResponseEntity<String> generateRandomAppId() {
        return ResponseEntity.ok(CryptotoolUtils.TestUtils.generateRandomAppId());
    }

    @PostMapping("/serial")
    public ResponseEntity<String> generateRandomSerial() {
        return ResponseEntity.ok(CryptotoolUtils.TestUtils.generateRandomSerial());
    }

    @PostMapping("/issuer_name")
    public ResponseEntity<String> generateRandomIssuerInHex() {
        return ResponseEntity.ok(CryptotoolUtils.TestUtils.generateRandomIssuerInHex());
    }

    @PostMapping("/hex")
    public ResponseEntity<String> generateRandomHexString(
            @RequestParam(value = "byte_count", required = false) Integer byteCountOrNull) {

        final int byteCount = Optional.ofNullable(byteCountOrNull)
                .filter(i -> i > 0)
                .orElse(16);

        return ResponseEntity.ok(CryptotoolUtils.TestUtils.generateRandomHexString(byteCount));
    }

}
