package org.amv.access.api.internal;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
//@ApiIgnore("This is an internal endpoint")
@RequestMapping(value = "/internal/hello")
public class HelloCtrl {

    @GetMapping
    public ResponseEntity<HelloWorldDto> index() {
        return ResponseEntity.ok(HelloWorldDto.builder()
                .build());
    }

    @Value
    @Builder
    public static class HelloWorldDto {
        private String message = "Hello World";
    }
}