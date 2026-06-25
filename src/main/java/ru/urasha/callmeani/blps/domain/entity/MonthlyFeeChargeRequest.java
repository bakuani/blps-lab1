package ru.urasha.callmeani.blps.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;

@Entity
@Table(name = "monthly_fee_charge_request")
@Getter
@Setter
@NoArgsConstructor
public class MonthlyFeeChargeRequest extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long subscriberId;

    @Column(nullable = false)
    private String billingPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessRequestStatus status;

    @Column
    private String errorMessage;

    @Column(nullable = false)
    private int attemptCount;

    @Column
    private String processInstanceId;

    @Column
    private Long dolibarrThirdPartyId;

    @Column
    private Long dolibarrInvoiceId;

    @Column
    private String dolibarrInvoiceRef;

}
