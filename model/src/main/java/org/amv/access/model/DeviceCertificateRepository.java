package org.amv.access.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "device_certificate", path = "model-device-certificate")
public interface DeviceCertificateRepository extends JpaRepository<DeviceCertificateEntity, Long> {

    Optional<DeviceCertificateEntity> findOneByDeviceIdAndApplicationId(@Param("deviceId") long deviceId,
                                                                        @Param("applicationId") long applicationId);

    Page<DeviceCertificateEntity> findByDeviceId(@Param("deviceId") long deviceId, Pageable page);

    Page<DeviceCertificateEntity> findByIssuerId(@Param("issuerId") long issuerId, Pageable page);

    Page<DeviceCertificateEntity> findByApplicationId(@Param("applicationId") long applicationId, Pageable page);

}
