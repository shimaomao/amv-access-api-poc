package org.amv.access.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "device_certificate", path = "device_certificate")
public interface DeviceCertificateRepository extends JpaRepository<DeviceCertificateEntity, Long> {

}
