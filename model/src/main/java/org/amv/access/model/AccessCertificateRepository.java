package org.amv.access.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "access_certificate", path = "model-access-certificate")
public interface AccessCertificateRepository extends JpaRepository<AccessCertificateEntity, Long> {

    Page<AccessCertificateEntity> findByDeviceIdAndVehicleId(@Param("deviceId") long deviceId, @Param("vehicleId") long vehicleId, Pageable page);

    Page<AccessCertificateEntity> findByDeviceId(@Param("deviceId") long deviceId, Pageable page);

    Page<AccessCertificateEntity> findByVehicleId(@Param("vehicleId") long vehicleId, Pageable page);

    Optional<AccessCertificateEntity> findByUuid(@Param("uuid") String uuid);
}
