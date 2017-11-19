package org.amv.access.api.internal;

import lombok.Builder;
import lombok.Getter;
import org.amv.access.exception.BadRequestException;
import org.amv.access.util.MoreHex;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/endocding")
public class EncodingCtrl {
    @GetMapping("/hex/{value}/base64")
    public ResponseEntity<EncodedValueDto> hexAsBase64(@PathVariable("value") String value) {
        String lowercasedValue = value.toLowerCase();
        if (!MoreHex.isHex(lowercasedValue)) {
            throw new BadRequestException("given `value` is not a hex value");
        }

        String encodedValue = CryptotoolUtils.encodeHexAsBase64(lowercasedValue);

        return ResponseEntity.ok(EncodedValueDto.builder()
                .value(value)
                .encoded(encodedValue)
                .sourceCodec("hex")
                .targetCodec("base64")
                .build());
    }

    @GetMapping("/base64/{value}/hex")
    public ResponseEntity<EncodedValueDto> base64AsHex(@PathVariable("value") String value) {
        String encodedValue = CryptotoolUtils.decodeBase64AsHex(value);

        return ResponseEntity.ok(EncodedValueDto.builder()
                .value(value)
                .encoded(encodedValue)
                .sourceCodec("base64")
                .targetCodec("hex")
                .build());
    }

    @Getter
    @Builder
    public static class EncodedValueDto {
        private String value;
        private String encoded;
        private String sourceCodec;
        private String targetCodec;
    }

}
