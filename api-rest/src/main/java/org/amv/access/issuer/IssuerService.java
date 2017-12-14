package org.amv.access.issuer;

import org.amv.access.model.IssuerEntity;

import java.util.Optional;
import java.util.UUID;

public interface IssuerService {
    Optional<IssuerEntity> findIssuerById(long id);

    Optional<IssuerEntity> findIssuerByUuid(UUID uuid);

    IssuerEntity findActiveIssuerOrThrow();
}
