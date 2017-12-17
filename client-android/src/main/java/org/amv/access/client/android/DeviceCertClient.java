package org.amv.access.client.android;

import io.reactivex.Observable;
import org.amv.access.client.android.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.android.model.CreateDeviceCertificateResponseDto;

/**
 * A client for accessing the <i>device_certificate</i> endpoint.
 */
public interface DeviceCertClient extends AccessApiClient {

    Observable<CreateDeviceCertificateResponseDto> createDeviceCertificate(
            String apiKey,
            CreateDeviceCertificateRequestDto createDeviceCertificateRequest);
}
