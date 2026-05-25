package ru.urasha.callmeani.blps.domain.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "tariff_change_request")
@Getter
@Setter
@NoArgsConstructor
public class TariffChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long subscriberId;

    @Column(nullable = false)
    private Long targetTariffId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tariff_change_request_option", joinColumns = @JoinColumn(name = "request_id"))
    @MapKeyColumn(name = "option_key")
    @Column(name = "option_value", nullable = false)
    private Map<String, String> options = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TariffChangeRequestStatus status;

    @Column
    private String errorMessage;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
