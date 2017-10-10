package org.amv.access.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "issuer", path = "issuer")
public interface IssuerRepository extends JpaRepository<IssuerEntity, Long> {

    Optional<IssuerEntity> findByNameAndPublicKeyBase64(String name, String publicKeyBase64);
}
