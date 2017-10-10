package org.amv.access.api.access;

import org.amv.access.api.access.model.CreateAccessCertificateRequest;
import org.amv.access.api.access.model.GetAccessCertificateRequest;
import org.amv.access.api.access.model.RevokeAccessCertificateRequest;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.core.AccessCertificate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccessCertificateService {
    Flux<AccessCertificate> getAccessCertificates(NonceAuthentication nonceAuthentication, GetAccessCertificateRequest request);

    Mono<AccessCertificate> createAccessCertificate(CreateAccessCertificateRequest request);

    Mono<Void> revokeAccessCertificate(NonceAuthentication nonceAuthentication, RevokeAccessCertificateRequest request);
}
