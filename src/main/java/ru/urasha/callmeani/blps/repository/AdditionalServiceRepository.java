package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.AdditionalService;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {
}
