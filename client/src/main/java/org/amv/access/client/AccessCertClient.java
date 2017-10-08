package org.amv.access.client;

import com.netflix.hystrix.HystrixCommand;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.amv.access.client.model.GetAccessCertificatesResponseDto;


/**
 * A client for accessing the <i>access_certificate</i> endpoint.
 *
 * @author Alois Leitner
 */
public interface AccessCertClient extends AmvAccessClient {

    @Headers({
            "amv-api-nonce: {nonce}",
            "amv-api-signature: {signedNonce}"
    })
    @RequestLine("GET /api/v1/device/{deviceSerialNumber}/access_certificates")
    HystrixCommand<GetAccessCertificatesResponseDto> fetchAccessCertificates(@Param("nonce") String nonce,
                                                                             @Param("signedNonce") String signedNonce,
                                                                             @Param("deviceSerialNumber") String deviceSerialNumber);


    @Headers({
            "amv-api-nonce: {nonce}",
            "amv-api-signature: {signedNonce}"
    })
    @RequestLine("DELETE /api/v1/device/{deviceSerialNumber}/access_certificates/{accessCertificateId}")
    HystrixCommand<Void> revokeAccessCertificate(@Param("nonce") String nonce,
                                                 @Param("signedNonce") String signedNonce,
                                                 @Param("deviceSerialNumber") String deviceSerialNumber,
                                                 @Param("accessCertificateId") String accessCertificateId);


}
