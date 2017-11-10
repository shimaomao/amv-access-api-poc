package org.amv.access.client.java6;

import com.netflix.hystrix.HystrixCommand;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.CreateDeviceCertificateResponseDto;


/**
 * A client for accessing the <i>device_certificate</i> endpoint.
 *
 */
public interface DeviceCertClient extends AccessApiClient {

    HystrixCommand<CreateDeviceCertificateResponseDto> createDeviceCertificate(
            String apiKey,
            CreateDeviceCertificateRequestDto createDeviceCertificateRequest);
}
