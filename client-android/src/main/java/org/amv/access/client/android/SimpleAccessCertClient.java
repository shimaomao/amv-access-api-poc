package org.amv.access.client.android;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import org.amv.access.client.android.model.GetAccessCertificatesResponseDto;


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
        return Observable.just(1)
                .flatMap(new Function<Integer, Observable<GetAccessCertificatesResponseDto>>() {
                    @Override
                    public Observable<GetAccessCertificatesResponseDto> apply(@NonNull Integer foo) throws Exception {
                        String url = String.format("%s/api/v1/device/%s/access_certificates", baseUrl, deviceSerialNumber);

                        Request request = new Request.Builder()
                                .addHeader(MoreHttpHeaders.AMV_NONCE, nonce)
                                .addHeader(MoreHttpHeaders.AMV_SIGNATURE, signedNonce)
                                .url(url)
                                .build();

                        Response response = null;
                        try {
                            response = client.newCall(request).execute();
                            return ResponseHelper.parse(response, GetAccessCertificatesResponseDto.class);
                        } catch (Exception e) {
                            return Observable.error(e);
                        } finally {
                            Util.closeQuietly(response);
                        }
                    }
                });
    }
}
