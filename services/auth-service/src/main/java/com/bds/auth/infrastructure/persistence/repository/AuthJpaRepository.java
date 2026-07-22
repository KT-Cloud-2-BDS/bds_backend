package com.bds.auth.infrastructure.persistence.repository;

import com.bds.auth.domain.entity.enums.Status;
import com.bds.auth.infrastructure.persistence.entity.AuthJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthJpaRepository extends JpaRepository<AuthJpaEntity, Long> {

    boolean existsByEmailAndStatus(String email, Status status);

    Optional<AuthJpaEntity> findByEmail(String email);

    Optional<AuthJpaEntity> findById(Long authId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AuthJpaEntity a SET a.status = :status, "
        + "a.updatedAt = CURRENT_TIMESTAMP, a.deletedAt = CURRENT_TIMESTAMP WHERE a.id = :authId")
    void softDeleteById(@Param("authId") Long authId, @Param("status") Status status);
}

