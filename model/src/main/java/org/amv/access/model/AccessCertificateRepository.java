package org.amv.access.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "access_certificate", path = "access_certificate")
public interface AccessCertificateRepository extends JpaRepository<AccessCertificateEntity, Long> {

    List<AccessCertificateEntity> findByDeviceIdAndVehicleId(long deviceId, long vehicleId);

    List<AccessCertificateEntity> findByDeviceId(long deviceId);

    List<AccessCertificateEntity> findByVehicleId(long vehicleId);

    Optional<AccessCertificateEntity> findByUuid(String uuid);
}
