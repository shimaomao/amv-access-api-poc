package org.amv.access.api.access;

import org.amv.access.api.access.model.CreateAccessCertificateRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.amv.access.util.SecureRandomUtils.generateRandomAppId;
import static org.amv.access.util.SecureRandomUtils.generateRandomSerial;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CreateAccessCertificateRequestEntityValidatorTest {

    private CreateAccessCertificateRequestValidator sut;

    @Before
    public void setUp() {
        this.sut = new CreateAccessCertificateRequestValidator();
    }

    @Test
    public void itShouldValidateWithSuccess() throws Exception {
        CreateAccessCertificateRequest request = CreateAccessCertificateRequest.builder()
                .appId(generateRandomAppId())
                .deviceSerialNumber(generateRandomSerial())
                .vehicleSerialNumber(generateRandomSerial())
                .build();

        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request,
                request.getClass().toString());

        this.sut.validate(request, errors);

        assertThat(errors.hasErrors(), is(false));
    }

}