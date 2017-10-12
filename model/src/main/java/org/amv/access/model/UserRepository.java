package org.amv.access.model;

import org.amv.access.model.projection.UserProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(excerptProjection = UserProjection.class, collectionResourceRel = "user", path = "model-user")
public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findByName(@Param("name") String name, Pageable page);
}
