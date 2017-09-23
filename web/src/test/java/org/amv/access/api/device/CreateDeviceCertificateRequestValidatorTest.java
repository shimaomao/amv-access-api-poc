package org.amv.access.api.device;

import org.amv.access.api.device.model.CreateDeviceCertificateRequest;
import org.amv.highmobility.cryptotool.BinaryExecutorImpl;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolImpl;
import org.amv.highmobility.cryptotool.CryptotoolOptionsImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.amv.highmobility.cryptotool.CryptotoolUtils.TestUtils.generateRandomAppId;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CreateDeviceCertificateRequestValidatorTest {

    private static Cryptotool cryptotool;

    private CreateDeviceCertificateRequestValidator sut;

    @BeforeClass
    public static void setUpClass() throws IOException, URISyntaxException {
        cryptotool = new CryptotoolImpl(
                CryptotoolOptionsImpl.builder()
                        .binaryExecutor(BinaryExecutorImpl.createDefault())
                        .build());
    }

    @Before
    public void setUp() {
        this.sut = new CreateDeviceCertificateRequestValidator();
    }

    @Test
    public void itShouldValidateWithSuccess() throws Exception {
        CreateDeviceCertificateRequest request = CreateDeviceCertificateRequest.builder()
                .appId(generateRandomAppId())
                .publicKey(cryptotool.generateKeys().block().getPublicKey())
                .build();

        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request,
                request.getClass().toString());

        this.sut.validate(request, errors);

        assertThat(errors.hasErrors(), is(false));
    }

}