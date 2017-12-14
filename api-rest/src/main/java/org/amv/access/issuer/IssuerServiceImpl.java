package org.amv.access.issuer;

import org.amv.access.model.IssuerEntity;
import org.amv.access.model.IssuerRepository;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;


public class IssuerServiceImpl implements IssuerService {

    private final IssuerRepository issuerRepository;

    public IssuerServiceImpl(IssuerRepository issuerRepository) {
        this.issuerRepository = requireNonNull(issuerRepository);
    }

    @Override
    public Optional<IssuerEntity> findIssuerById(long id) {
        try {
            return Optional.ofNullable(issuerRepository.findOne(id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<IssuerEntity> findIssuerByUuid(UUID uuid) {
        try {
            return issuerRepository.findByUuid(uuid.toString());
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public IssuerEntity findActiveIssuerOrThrow() {
        return issuerRepository
                .findFirstByOrderByCreatedAtDesc()
                .orElseThrow(() -> new IllegalStateException("Could not find active issuer"));
    }
}
