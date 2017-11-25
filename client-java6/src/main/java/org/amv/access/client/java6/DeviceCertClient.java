package org.amv.access.client.java6;

import io.reactivex.Observable;
import org.amv.access.client.model.java6.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.java6.CreateDeviceCertificateResponseDto;

/**
 * A client for accessing the <i>device_certificate</i> endpoint.
 */
public interface DeviceCertClient extends AccessApiClient {

    Observable<CreateDeviceCertificateResponseDto> createDeviceCertificate(
            String apiKey,
            CreateDeviceCertificateRequestDto createDeviceCertificateRequest);
}
