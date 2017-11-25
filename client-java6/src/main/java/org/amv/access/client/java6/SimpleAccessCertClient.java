package org.amv.access.client.java6;

import com.google.common.base.Preconditions;
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
        this.baseUrl = Preconditions.checkNotNull(baseUrl);
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

                    return Clients.defaultObjectMapper.fromJson(responseBody.charStream(), GetAccessCertificatesResponseDto.class);
                } finally {
                    Util.closeQuietly(response);
                }
            }
        });
    }

    @Override
    public Observable<Void> revokeAccessCertificate(final String nonce,
                                                    final String signedNonce,
                                                    final String deviceSerialNumber,
                                                    final String accessCertificateId) {
        return Observable.fromCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                String url = String.format("%s/api/v1/device/%s/access_certificates/%s", baseUrl, deviceSerialNumber, accessCertificateId);

                Request request = new Request.Builder()
                        .addHeader(MoreHttpHeaders.AMV_NONCE, nonce)
                        .addHeader(MoreHttpHeaders.AMV_SIGNATURE, signedNonce)
                        .url(url)
                        .delete()
                        .build();

                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    return null;
                } finally {
                    Util.closeQuietly(response);
                }
            }
        });
    }
}
