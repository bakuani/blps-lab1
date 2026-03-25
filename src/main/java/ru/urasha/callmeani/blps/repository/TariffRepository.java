package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.Tariff;

import java.util.List;

public interface TariffRepository extends JpaRepository<Tariff, Long> {

    List<Tariff> findByCategoryId(Long categoryId);

    List<Tariff> findByNameContainingIgnoreCase(String query);

    List<Tariff> findByCategoryIdAndNameContainingIgnoreCase(Long categoryId, String query);
}
