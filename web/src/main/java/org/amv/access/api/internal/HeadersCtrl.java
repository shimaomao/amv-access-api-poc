package org.amv.access.api.internal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/headers")
public class HeadersCtrl {

    @GetMapping
    public String headers(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames()).stream()
                .map(headerName -> headerName + ": " + request.getHeader(headerName))
                .collect(Collectors.joining(",\n"));
    }
}
