package org.amv.access.client.java6;

import com.netflix.hystrix.HystrixCommand;
import org.amv.access.client.model.GetAccessCertificatesResponseDto;


/**
 * A client for accessing the <i>access_certificate</i> endpoint.
 */
public interface AccessCertClient extends AccessApiClient {

    HystrixCommand<GetAccessCertificatesResponseDto> fetchAccessCertificates(String nonce,
                                                                             String signedNonce,
                                                                             String deviceSerialNumber);

    HystrixCommand<Void> revokeAccessCertificate(String nonce,
                                                 String signedNonce,
                                                 String deviceSerialNumber,
                                                 String accessCertificateId);


}
