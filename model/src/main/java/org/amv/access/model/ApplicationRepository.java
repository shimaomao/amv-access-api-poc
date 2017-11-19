package org.amv.access.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "application", path = "model-application")
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {

    Optional<ApplicationEntity> findOneByAppId(@Param("appId") String appId);

    Optional<ApplicationEntity> findOneByApiKey(@Param("apiKey") String apiKey);

    Page<ApplicationEntity> findByName(@Param("name") String name, Pageable page);
}
