package ru.urasha.callmeani.blps.service.eis;

import ru.urasha.callmeani.blps.domain.entity.Subscriber;

import java.math.BigDecimal;
import java.util.Optional;

public interface DolibarrInvoiceService {

    Optional<DolibarrInvoiceReference> createUnpaidMonthlyFeeInvoice(
        Subscriber subscriber,
        Long requestId,
        String billingPeriod,
        BigDecimal amount
    );

    boolean markInvoicePaid(Long invoiceId);

    record DolibarrInvoiceReference(Long thirdPartyId, Long invoiceId, String externalRef) {
    }
}
