package org.amv.access.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "issuer", path = "model-issuer")
public interface IssuerRepository extends JpaRepository<IssuerEntity, Long> {

    Page<IssuerEntity> findByName(@Param("name") String name, Pageable page);

    Optional<IssuerEntity> findByNameAndPublicKeyBase64(@Param("name") String name, @Param("publicKeyBase64") String publicKeyBase64);

    Optional<IssuerEntity> findFirstByOrderByCreatedDesc();
}
