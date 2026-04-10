package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.AdditionalFeature;

public interface AdditionalFeatureRepository extends JpaRepository<AdditionalFeature, Long> {
}

