package org.amv.access.client;

import com.netflix.hystrix.HystrixCommand;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.DeviceCertificateDto;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;


/**
 * A client for accessing the <i>device_certificate</i> endpoint.
 *
 * @author Alois Leitner
 */
public interface DeviceCertClient extends AmvAccessClient {

    @Headers({
            CONTENT_TYPE + ": " + "application/json;charset=UTF-8",
            AUTHORIZATION + ": " + "{apiKey}"
    })
    @RequestLine("POST /api/v1/device_certificates")
    HystrixCommand<DeviceCertificateDto> createDeviceCertificate(
            @Param("apiKey") String apiKey,
            CreateDeviceCertificateRequestDto createDeviceCertificateRequest);
}
