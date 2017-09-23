package org.amv.access.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(collectionResourceRel = "access_certificate", path = "access_certificate")
public interface AccessCertificateRepository extends JpaRepository<AccessCertificate, Long> {

    List<AccessCertificate> findByDeviceSerialNumberAndVehicleSerialNumber(String deviceSerialNumber,
                                                                           String vehicleSerialNumber);

    List<AccessCertificate> findByDeviceSerialNumber(String deviceSerialNumber);

    List<AccessCertificate> findByVehicleSerialNumber(String vehicleSerialNumber);
}
