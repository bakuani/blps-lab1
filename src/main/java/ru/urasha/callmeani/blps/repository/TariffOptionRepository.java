package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.TariffOption;

import java.util.List;

public interface TariffOptionRepository extends JpaRepository<TariffOption, Long> {
}
