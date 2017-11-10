package org.amv.access.client.java6;

import com.google.common.base.Preconditions;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import okhttp3.*;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.CreateDeviceCertificateResponseDto;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;

public class SimpleDeviceCertClient implements DeviceCertClient {
    private static final HystrixCommandGroupKey post = HystrixCommandGroupKey.Factory.asKey("post-create-device-cert");

    private final OkHttpClient client = new OkHttpClient();

    private final String baseUrl;

    SimpleDeviceCertClient(String baseUrl) {
        this.baseUrl = Preconditions.checkNotNull(baseUrl);
    }

    @Override
    public HystrixCommand<CreateDeviceCertificateResponseDto> createDeviceCertificate(final String apiKey,
                                                                                      final CreateDeviceCertificateRequestDto createDeviceCertificateRequest) {

        return new HystrixCommand<CreateDeviceCertificateResponseDto>(post) {
            @Override
            protected CreateDeviceCertificateResponseDto run() throws Exception {
                String url = String.format("%s/api/v1/device_certificates", baseUrl);

                String json = Clients.defaultObjectMapper.writeValueAsString(createDeviceCertificateRequest);
                RequestBody requestBody = RequestBody.create(Clients.JSON, json);

                Request request = new Request.Builder()
                        .addHeader(AUTHORIZATION, apiKey)
                        .url(url)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();

                final ResponseBody responseBody = response.body();

                final CreateDeviceCertificateResponseDto result = Clients.defaultObjectMapper.readerFor(CreateDeviceCertificateResponseDto.class)
                        .readValue(responseBody.bytes());

                return result;
            }
        };
    }
}
