package ru.urasha.callmeani.blps.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;

import java.time.OffsetDateTime;

@Entity
@Table(name = "subscriber_service")
@Getter
@Setter
@NoArgsConstructor
public class SubscriberFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private Subscriber subscriber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private AdditionalFeature service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriberFeatureStatus status;

    @Column(nullable = false)
    private OffsetDateTime connectedAt;

    private OffsetDateTime disabledAt;
}

