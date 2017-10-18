package org.amv.access.issuer;

import org.amv.access.model.IssuerEntity;

import java.util.Optional;

public interface IssuerService {
    Optional<IssuerEntity> findIssuerById(long id);

    IssuerEntity findActiveIssuerOrThrow();
}
