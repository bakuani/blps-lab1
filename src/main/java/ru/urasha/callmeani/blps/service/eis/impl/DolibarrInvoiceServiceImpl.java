package ru.urasha.callmeani.blps.service.eis.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.service.eis.DolibarrInvoiceService;
import ru.urasha.callmeani.blps.service.eis.DolibarrSubscriberService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DolibarrInvoiceServiceImpl implements DolibarrInvoiceService {

    private static final Map<String, Object> VALIDATE_PAYLOAD = Map.of(
        "idwarehouse", 0,
        "notrigger", 0
    );

    private final DolibarrSubscriberService dolibarrSubscriberService;
    @Qualifier("dolibarrRestClient")
    private final RestClient dolibarrRestClient;

    @Override
    public Optional<DolibarrInvoiceReference> createUnpaidMonthlyFeeInvoice(
        Subscriber subscriber,
        Long requestId,
        String billingPeriod,
        BigDecimal amount
    ) {
        if (subscriber == null || requestId == null) {
            return Optional.empty();
        }

        Long thirdPartyId = dolibarrSubscriberService.ensureThirdPartyId(subscriber);
        if (thirdPartyId == null) {
            log.warn(
                "Dolibarr invoice creation skipped: requestId={}, subscriberId={}, reason=thirdparty_not_found",
                requestId,
                subscriber.getId()
            );
            return Optional.empty();
        }

        String externalRef = buildExternalRef(requestId, billingPeriod);
        Map<String, Object> payload = buildInvoicePayload(thirdPartyId, subscriber, externalRef, billingPeriod, amount);

        try {
            Long invoiceId = createInvoice(payload);
            if (invoiceId == null) {
                log.warn(
                    "Dolibarr invoice creation failed: requestId={}, subscriberId={}, reason=empty_invoice_id",
                    requestId,
                    subscriber.getId()
                );
                return Optional.empty();
            }

            addMonthlyFeeLine(invoiceId, billingPeriod, amount);
            validateInvoice(invoiceId);

            log.info(
                "Dolibarr monthly fee invoice created: requestId={}, subscriberId={}, thirdPartyId={}, invoiceId={}, externalRef={}",
                requestId,
                subscriber.getId(),
                thirdPartyId,
                invoiceId,
                externalRef
            );
            return Optional.of(new DolibarrInvoiceReference(thirdPartyId, invoiceId, externalRef));
        } catch (RestClientResponseException ex) {
            log.warn(
                "Dolibarr invoice creation failed: requestId={}, subscriberId={}, statusCode={}, body={}",
                requestId,
                subscriber.getId(),
                ex.getStatusCode().value(),
                ex.getResponseBodyAsString()
            );
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn(
                "Dolibarr invoice creation failed: requestId={}, subscriberId={}, reason=network_error, message={}",
                requestId,
                subscriber.getId(),
                ex.getMessage()
            );
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn(
                "Dolibarr invoice creation failed: requestId={}, subscriberId={}, reason=unexpected_error, message={}",
                requestId,
                subscriber.getId(),
                ex.getMessage()
            );
            return Optional.empty();
        }
    }

    @Override
    public boolean markInvoicePaid(Long invoiceId) {
        if (invoiceId == null) {
            return false;
        }
        try {
            validateInvoice(invoiceId);
        } catch (RestClientException ex) {
            log.warn(
                "Dolibarr invoice pre-pay validation failed: invoiceId={}, reason={}",
                invoiceId,
                ex.getMessage()
            );
        }
        return callInvoiceStateEndpoint(invoiceId, "settopaid");
    }

    private boolean callInvoiceStateEndpoint(Long invoiceId, String action) {
        if (invoiceId == null) {
            return false;
        }
        try {
            dolibarrRestClient.post()
                .uri("/invoices/{id}/" + action, invoiceId)
                .retrieve()
                .toBodilessEntity();
            return true;
        } catch (RestClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            log.warn(
                "Dolibarr invoice state update failed: invoiceId={}, action={}, statusCode={}, body={}",
                invoiceId,
                action,
                statusCode.value(),
                ex.getResponseBodyAsString()
            );
            return false;
        } catch (RestClientException ex) {
            log.warn(
                "Dolibarr invoice state update failed: invoiceId={}, action={}, reason=network_error, message={}",
                invoiceId,
                action,
                ex.getMessage()
            );
            return false;
        } catch (RuntimeException ex) {
            log.warn(
                "Dolibarr invoice state update failed: invoiceId={}, action={}, reason=unexpected_error, message={}",
                invoiceId,
                action,
                ex.getMessage()
            );
            return false;
        }
    }

    private Long createInvoice(Map<String, Object> payload) {
        Object response = dolibarrRestClient.post()
            .uri("/invoices")
            .body(payload)
            .retrieve()
            .body(Object.class);
        if (response instanceof Map<?, ?> map) {
            Long id = parseLong(map.get("id"));
            if (id != null) {
                return id;
            }
            return parseLong(map.get("rowid"));
        }
        return parseLong(response);
    }

    private void validateInvoice(Long invoiceId) {
        dolibarrRestClient.post()
            .uri("/invoices/{id}/validate", invoiceId)
            .body(VALIDATE_PAYLOAD)
            .retrieve()
            .toBodilessEntity();
    }

    private void addMonthlyFeeLine(Long invoiceId, String billingPeriod, BigDecimal amount) {
        Map<String, Object> linePayload = new LinkedHashMap<>();
        linePayload.put("desc", "Monthly fee for period " + safe(billingPeriod));
        linePayload.put("subprice", normalizeNumericAmount(amount));
        linePayload.put("qty", 1);
        linePayload.put("tva_tx", 0);
        linePayload.put("product_type", 1);

        dolibarrRestClient.post()
            .uri("/invoices/{id}/lines", invoiceId)
            .body(linePayload)
            .retrieve()
            .toBodilessEntity();
    }

    private Map<String, Object> buildInvoicePayload(
        Long thirdPartyId,
        Subscriber subscriber,
        String externalRef,
        String billingPeriod,
        BigDecimal amount
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("socid", thirdPartyId);
        payload.put("type", 0);
        payload.put("ref_ext", externalRef);
        payload.put("date", OffsetDateTime.now().toEpochSecond());
        payload.put("note_public", "Monthly fee for " + safe(billingPeriod));
        payload.put("note_private", "subscriberId=" + subscriber.getId() + ", amount=" + normalizeAmount(amount));
        return payload;
    }

    private String buildExternalRef(Long requestId, String billingPeriod) {
        String sanitizedPeriod = safe(billingPeriod)
            .replace(':', '-')
            .replace(' ', '_');
        return "MFC-" + requestId + "-" + sanitizedPeriod;
    }

    private String normalizeAmount(BigDecimal amount) {
        return amount == null ? "0.00" : amount.stripTrailingZeros().toPlainString();
    }

    private BigDecimal normalizeNumericAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.max(BigDecimal.ZERO);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(Objects.toString(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
