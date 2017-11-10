package org.amv.access.client.java6;

import com.google.common.base.Preconditions;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import org.amv.access.client.MoreHttpHeaders;
import org.amv.access.client.model.GetAccessCertificatesResponseDto;

public class SimpleAccessCertClient implements AccessCertClient {
    private static final HystrixCommandGroupKey get = HystrixCommandGroupKey.Factory.asKey("GET-access-cert");
    private static final HystrixCommandGroupKey delete = HystrixCommandGroupKey.Factory.asKey("DELETE-access-cert");

    private final OkHttpClient client = new OkHttpClient();

    private final String baseUrl;

    SimpleAccessCertClient(String baseUrl) {
        this.baseUrl = Preconditions.checkNotNull(baseUrl);
    }

    @Override
    public HystrixCommand<GetAccessCertificatesResponseDto> fetchAccessCertificates(final String nonce,
                                                                                    final String signedNonce,
                                                                                    final String deviceSerialNumber) {
        return new HystrixCommand<GetAccessCertificatesResponseDto>(get) {
            @Override
            protected GetAccessCertificatesResponseDto run() throws Exception {
                String url = String.format("%s/api/v1/device/%s/access_certificates", baseUrl, deviceSerialNumber);

                Request request = new Request.Builder()
                        .addHeader(MoreHttpHeaders.AMV_NONCE, nonce)
                        .addHeader(MoreHttpHeaders.AMV_SIGNATURE, signedNonce)
                        .url(url)
                        .build();

                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    final ResponseBody responseBody = response.body();

                    return Clients.defaultObjectMapper.readerFor(GetAccessCertificatesResponseDto.class)
                            .readValue(responseBody.bytes());
                } finally {
                    Util.closeQuietly(response);
                }
            }
        };
    }

    @Override
    public HystrixCommand<Void> revokeAccessCertificate(final String nonce,
                                                        final String signedNonce,
                                                        final String deviceSerialNumber,
                                                        final String accessCertificateId) {
        return new HystrixCommand<Void>(delete) {
            @Override
            protected Void run() throws Exception {
                String url = String.format("%s/api/v1/device/%s/access_certificates/%s", baseUrl, deviceSerialNumber, accessCertificateId);

                Request request = new Request.Builder()
                        .addHeader(MoreHttpHeaders.AMV_NONCE, nonce)
                        .addHeader(MoreHttpHeaders.AMV_SIGNATURE, signedNonce)
                        .url(url)
                        .build();

                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    return null;
                } finally {
                    Util.closeQuietly(response);
                }
            }
        };
    }
}
