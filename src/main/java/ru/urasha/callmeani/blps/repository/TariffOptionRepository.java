package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.TariffOption;

public interface TariffOptionRepository extends JpaRepository<TariffOption, Long> {
}
