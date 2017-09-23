package org.amv.access.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "access_certificate_request", path = "access_certificate_request")
public interface AccessCertificateRequestRepository extends JpaRepository<AccessCertificateRequest, Long> {

}
