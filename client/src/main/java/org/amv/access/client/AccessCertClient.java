package org.amv.access.client;

import com.netflix.hystrix.HystrixCommand;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.amv.access.client.model.CreateAccessCertificateRequestDto;
import org.amv.access.client.model.CreateAccessCertificateResponseDto;
import org.amv.access.client.model.GetAccessCertificatesResponseDto;
import org.amv.access.client.model.UpdateAccessCertificateRequestDto;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;


/**
 * A client for accessing the <i>access_certificate</i> endpoint.
 *
 * @author Alois Leitner
 */
public interface AccessCertClient extends AccessApiClient {

    @Headers({
            MoreHttpHeaders.AMV_NONCE + ": " + "{nonce}",
            MoreHttpHeaders.AMV_SIGNATURE + ": " + "{signedNonce}"
    })
    @RequestLine("GET /api/v1/device/{deviceSerialNumber}/access_certificates")
    HystrixCommand<GetAccessCertificatesResponseDto> fetchAccessCertificates(@Param("nonce") String nonce,
                                                                             @Param("signedNonce") String signedNonce,
                                                                             @Param("deviceSerialNumber") String deviceSerialNumber);

    @Headers({
            CONTENT_TYPE + ": " + "application/json;charset=UTF-8",
            MoreHttpHeaders.AMV_NONCE + ": " + "{nonce}",
            MoreHttpHeaders.AMV_SIGNATURE + ": " + "{signedNonce}"
    })
    @RequestLine("POST /api/v1/issuer/{issuerUuid}/access_certificates")
    HystrixCommand<CreateAccessCertificateResponseDto> createAccessCertificate(
            @Param("nonce") String nonce,
            @Param("signedNonce") String signedNonce,
            @Param("issuerUuid") String issuerUuid,
            CreateAccessCertificateRequestDto body);

    @Headers({
            CONTENT_TYPE + ": " + "application/json;charset=UTF-8",
            MoreHttpHeaders.AMV_NONCE + ": " + "{nonce}",
            MoreHttpHeaders.AMV_SIGNATURE + ": " + "{signedNonce}"
    })
    @RequestLine("PUT /api/v1/issuer/{issuerUuid}/access_certificates/{accessCertificateId}/signature")
    HystrixCommand<Boolean> addAccessCertificateSignature(
            @Param("nonce") String nonce,
            @Param("signedNonce") String signedNonce,
            @Param("issuerUuid") String issuerUuid,
            @Param("accessCertificateId") String accessCertificateId,
            UpdateAccessCertificateRequestDto body);

    @Headers({
            MoreHttpHeaders.AMV_NONCE + ": " + "{nonce}",
            MoreHttpHeaders.AMV_SIGNATURE + ": " + "{signedNonce}"
    })
    @RequestLine("DELETE /api/v1/issuer/{issuerUuid}/access_certificates/{accessCertificateId}")
    HystrixCommand<Boolean> revokeAccessCertificate(@Param("nonce") String nonce,
                                                    @Param("signedNonce") String signedNonce,
                                                    @Param("issuerUuid") String issuerUuid,
                                                    @Param("accessCertificateId") String accessCertificateId);

}