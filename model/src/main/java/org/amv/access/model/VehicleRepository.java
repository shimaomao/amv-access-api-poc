package org.amv.access.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "vehicle", path = "vehicle")
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findBySerialNumber(String serial);
}
