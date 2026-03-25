package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.TariffCategory;

public interface TariffCategoryRepository extends JpaRepository<TariffCategory, Long> {
}
