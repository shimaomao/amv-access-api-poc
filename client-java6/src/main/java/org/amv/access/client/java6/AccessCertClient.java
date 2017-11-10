package org.amv.access.client.java6;


import org.amv.access.client.model.java6.GetAccessCertificatesResponseDto;
import rx.Observable;


/**
 * A client for accessing the <i>access_certificate</i> endpoint.
 */
public interface AccessCertClient extends AccessApiClient {

    Observable<GetAccessCertificatesResponseDto> fetchAccessCertificates(String nonce,
                                                                         String signedNonce,
                                                                         String deviceSerialNumber);

    Observable<Void> revokeAccessCertificate(String nonce,
                                             String signedNonce,
                                             String deviceSerialNumber,
                                             String accessCertificateId);
}