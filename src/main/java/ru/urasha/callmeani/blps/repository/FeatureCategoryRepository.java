package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.FeatureCategory;

public interface FeatureCategoryRepository extends JpaRepository<FeatureCategory, Long> {
}

