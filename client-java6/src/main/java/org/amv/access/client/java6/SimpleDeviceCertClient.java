package org.amv.access.client.java6;

import com.google.common.base.Preconditions;
import okhttp3.*;
import okhttp3.internal.Util;
import org.amv.access.client.model.java6.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.java6.CreateDeviceCertificateResponseDto;
import rx.Observable;

import java.util.concurrent.Callable;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;

public class SimpleDeviceCertClient implements DeviceCertClient {

    private final OkHttpClient client = new OkHttpClient();

    private final String baseUrl;

    SimpleDeviceCertClient(String baseUrl) {
        this.baseUrl = Preconditions.checkNotNull(baseUrl);
    }

    @Override
    public Observable<CreateDeviceCertificateResponseDto> createDeviceCertificate(final String apiKey,
                                                                                  final CreateDeviceCertificateRequestDto createDeviceCertificateRequest) {

        return Observable.fromCallable(new Callable<CreateDeviceCertificateResponseDto>() {
            @Override
            public CreateDeviceCertificateResponseDto call() throws Exception {

                String url = String.format("%s/api/v1/device_certificates", baseUrl);

                String json = Clients.defaultObjectMapper.toJson(createDeviceCertificateRequest);
                RequestBody requestBody = RequestBody.create(Clients.JSON, json);

                Request request = new Request.Builder()
                        .addHeader(AUTHORIZATION, apiKey)
                        .url(url)
                        .post(requestBody)
                        .build();

                Response response = null;
                try {
                    response = client.newCall(request).execute();

                    final ResponseBody responseBody = response.body();

                    return Clients.defaultObjectMapper.fromJson(responseBody.charStream(), CreateDeviceCertificateResponseDto.class);
                } finally {
                    Util.closeQuietly(response);
                }
            }
        });
    }
}
