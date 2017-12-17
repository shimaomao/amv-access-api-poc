package org.amv.access.client;

import com.netflix.hystrix.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.CreateDeviceCertificateResponseDto;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@Slf4j
@Ignore("Unignore on demand")
public class DeviceCertClientIT {

    private static final String BASE_ENDPOINT = "https://www.example.com";
    private static final String DEMO_APP_ID = "0000123456789abcdef00000";
    private static final String DEMO_API_KEY = "demodemodemo";
    private static final String ANY_PUBLIC_KEY = "QJ+HNttBDWrYkJfelkH8EfZkR/7uDCdZgIC0vkthqVNxZ51Q6tsh20mNPPWFlhPGgXau+LZm/O44btkkmLxgSA==";

    @Test
    public void itShouldCreateDeviceCertificateSuccessfully() {
        DeviceCertClient sut = Clients.simpleDeviceCertClient(BASE_ENDPOINT);

        CreateDeviceCertificateRequestDto request = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey(ANY_PUBLIC_KEY)
                .build();

        HystrixCommand<CreateDeviceCertificateResponseDto> deviceCertificateRequest = sut
                .createDeviceCertificate(DEMO_APP_ID, DEMO_API_KEY, request);

        CreateDeviceCertificateResponseDto deviceCertificateResponse = deviceCertificateRequest.execute();

        assertThat(deviceCertificateResponse, is(notNullValue()));
    }

    @Test
    public void itShouldCreateDeviceCertificateSuccessfullyReactive() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        DeviceCertClient sut = Clients.simpleDeviceCertClient(BASE_ENDPOINT);

        CreateDeviceCertificateRequestDto request = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey(ANY_PUBLIC_KEY)
                .build();

        sut.createDeviceCertificate(DEMO_APP_ID, DEMO_API_KEY, request)
                .toObservable()
                .subscribe(response -> {
                    log.info("Ok, got: {}", response);
                }, error -> {
                    log.error("", error);
                    latch.countDown();
                }, latch::countDown);

        latch.await();
    }
}
