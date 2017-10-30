package org.amv.access.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "vehicle", path = "model-vehicle")
public interface VehicleRepository extends JpaRepository<VehicleEntity, Long> {

    Optional<VehicleEntity> findOneBySerialNumber(@Param("serialNumber") String serial);

    Page<VehicleEntity> findByName(@Param("name") String name, Pageable page);
}
