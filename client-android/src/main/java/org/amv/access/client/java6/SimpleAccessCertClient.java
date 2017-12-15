package org.amv.access.client.java6;

import io.reactivex.Observable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import org.amv.access.client.MoreHttpHeaders;
import org.amv.access.client.model.java6.GetAccessCertificatesResponseDto;

import java.util.concurrent.Callable;


public class SimpleAccessCertClient implements AccessCertClient {

    private final OkHttpClient client = new OkHttpClient();

    private final String baseUrl;

    SimpleAccessCertClient(String baseUrl) {
        if (baseUrl == null) {
            throw new IllegalArgumentException("`baseUrl` must not be null");
        }
        this.baseUrl = baseUrl;
    }

    @Override
    public Observable<GetAccessCertificatesResponseDto> fetchAccessCertificates(final String nonce,
                                                                                final String signedNonce,
                                                                                final String deviceSerialNumber) {
        return Observable.fromCallable(new Callable<GetAccessCertificatesResponseDto>() {
            @Override
            public GetAccessCertificatesResponseDto call() throws Exception {
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

                    return Clients.defaultObjectMapper
                            .fromJson(responseBody.charStream(), GetAccessCertificatesResponseDto.class);
                } finally {
                    Util.closeQuietly(response);
                }
            }
        });
    }
}
