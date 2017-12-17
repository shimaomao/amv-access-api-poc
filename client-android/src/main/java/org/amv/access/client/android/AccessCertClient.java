package org.amv.access.client.android;


import io.reactivex.Observable;
import org.amv.access.client.android.model.GetAccessCertificatesResponseDto;

/**
 * A client for accessing the <i>access_certificate</i> endpoint.
 */
public interface AccessCertClient extends AccessApiClient {

    Observable<GetAccessCertificatesResponseDto> fetchAccessCertificates(String nonce,
                                                                         String signedNonce,
                                                                         String deviceSerialNumber);
}
