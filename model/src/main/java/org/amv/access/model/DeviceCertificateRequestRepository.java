package org.amv.access.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "device_certificate_request", path = "model-device-certificate-request")
public interface DeviceCertificateRequestRepository extends JpaRepository<DeviceCertificateRequest, Long> {

}
