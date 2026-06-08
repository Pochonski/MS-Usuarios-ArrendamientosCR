package com.arrendamientos.usuarios.infrastructure.persistence.repository;

import com.arrendamientos.usuarios.infrastructure.persistence.entity.SequenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SequenceJpaRepository extends JpaRepository<SequenceEntity, String> {

    @Modifying
    @Query(value = "UPDATE Sequences SET CurrentValue = CurrentValue + 1 WHERE Name = :name", nativeQuery = true)
    int incrementar(@Param("name") String name);
}
