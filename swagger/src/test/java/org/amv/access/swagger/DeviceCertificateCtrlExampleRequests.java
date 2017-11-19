package org.amv.access.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.amv.access.api.auth.ApplicationAuthenticationArgumentResolver;
import org.amv.access.api.device.DeviceCertificateCtrl;
import org.amv.access.certificate.DeviceCertificateService;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.core.impl.DeviceCertificateImpl;
import org.amv.access.demo.DemoService;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.model.DeviceEntity;
import org.amv.access.model.IssuerEntity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class DeviceCertificateCtrlExampleRequests {

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation(
            "build/docs/swagger/examples");

    private MockMvc mvc;

    private ObjectMapper mapper = new ObjectMapper() {{
        this.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    }};

    private DeviceCertificateService deviceCertificateServiceMock;
    private DemoService demoServiceMock;

    @Before
    public void setUp() {
        this.deviceCertificateServiceMock = mock(DeviceCertificateService.class);
        this.demoServiceMock = mock(DemoService.class);

        DeviceCertificateCtrl sut = new DeviceCertificateCtrl(deviceCertificateServiceMock, demoServiceMock);

        RestDocumentationResultHandler document = document("{method-name}",
                preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));

        ApplicationAuthenticationArgumentResolver.ApiKeyResolver apiKeyResolver = apiKey ->
                Mono.just(new ApplicationEntity() {{
                    setAppId("demo");
                    setApiKey("demodemodemo");
                }});

        this.mvc = MockMvcBuilders.standaloneSetup(sut)
                .setCustomArgumentResolvers(new ApplicationAuthenticationArgumentResolver(apiKeyResolver))
                .apply(documentationConfiguration(this.restDocumentation))
                .alwaysDo(document)
                .alwaysDo(print())
                .build();
    }

    @Test
    public void postDeviceCertificates() throws Exception {
        when(deviceCertificateServiceMock.createDeviceCertificate(any(), any()))
                .thenReturn(Mono.just(DeviceCertificateImpl.builder()
                        .application(ApplicationEntity.builder()
                                .build())
                        .device(DeviceEntity.builder()
                                .build())
                        .issuer(IssuerEntity.builder()
                                .publicKeyBase64("ISSUER_PUBLIC_KEY")
                                .build())
                        .signedDeviceCertificateBase64("DEVICE_CERTIFICATE")
                        .build()));

        when(demoServiceMock.createDemoAccessCertificateIfNecessary(any()))
                .thenReturn(Mono.empty());

        CreateDeviceCertificateRequestDto body = CreateDeviceCertificateRequestDto.builder()
                .devicePublicKey("any")
                .build();

        this.mvc.perform(post("/api/v1/device_certificates")
                .content(mapper.writeValueAsString(body))
                .header(HttpHeaders.AUTHORIZATION, "demodemodemo")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .with(setupRequest()))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.device_certificate.device_certificate").value("DEVICE_CERTIFICATE"))
                .andExpect(jsonPath("$.device_certificate.issuer_public_key").value("ISSUER_PUBLIC_KEY"));
    }

    private RequestPostProcessor setupRequest() {
        return request -> {
            request.setRemoteAddr("127.0.0.1");
            return request;
        };
    }
}
