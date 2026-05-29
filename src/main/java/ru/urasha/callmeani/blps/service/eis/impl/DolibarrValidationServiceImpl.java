package ru.urasha.callmeani.blps.service.eis.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.urasha.callmeani.blps.domain.entity.FeatureDisableRequest;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.TariffChangeRequest;
import ru.urasha.callmeani.blps.config.DolibarrProperties;
import ru.urasha.callmeani.blps.service.eis.EisValidationService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DolibarrValidationServiceImpl implements EisValidationService {

    private final SubscriberService subscriberService;
    @Qualifier("dolibarrRestClient")
    private final RestClient dolibarrRestClient;
    private final DolibarrProperties dolibarrProperties;

    @Override
    public boolean allowTariffChange(TariffChangeRequest request) {
        return validateSubscriber(request.getSubscriberId(), "tariff_change", request.getId());
    }

    @Override
    public boolean allowFeatureDisable(FeatureDisableRequest request) {
        return validateSubscriber(request.getSubscriberId(), "feature_disable", request.getId());
    }

    private boolean validateSubscriber(Long subscriberId, String operationType, Long requestId) {
        try {
            Subscriber subscriber = subscriberService.getSubscriberEntity(subscriberId);
            String normalizedPhone = normalizePhone(subscriber.getPhone());
            if (normalizedPhone == null || normalizedPhone.isBlank()) {
                log.warn(
                    "Dolibarr validation rejected: operationType={}, requestId={}, subscriberId={}, reason=empty_phone",
                    operationType, requestId, subscriberId
                );
                return false;
            }

            Long thirdPartyId = findThirdPartyIdByPhone(normalizedPhone, operationType, requestId, subscriberId);
            if (thirdPartyId == null) {
                log.warn(
                    "Dolibarr validation rejected: operationType={}, requestId={}, subscriberId={}, reason=thirdparty_not_found",
                    operationType, requestId, subscriberId
                );
                return false;
            }

            boolean hasUnpaidInvoices = hasUnpaidInvoices(thirdPartyId);
            if (hasUnpaidInvoices) {
                log.info(
                    "Dolibarr validation rejected: operationType={}, requestId={}, subscriberId={}, thirdPartyId={}, reason=unpaid_invoices",
                    operationType, requestId, subscriberId, thirdPartyId
                );
                return false;
            }

            log.info(
                "Dolibarr validation allowed: operationType={}, requestId={}, subscriberId={}, thirdPartyId={}",
                operationType, requestId, subscriberId, thirdPartyId
            );
            return true;
        } catch (RestClientResponseException ex) {
            return resolveTechnicalFailure(
                operationType,
                requestId,
                subscriberId,
                "http_status_" + ex.getStatusCode().value(),
                ex
            );
        } catch (RestClientException ex) {
            return resolveTechnicalFailure(operationType, requestId, subscriberId, "network_error", ex);
        } catch (RuntimeException ex) {
            return resolveTechnicalFailure(operationType, requestId, subscriberId, "unexpected_error", ex);
        }
    }

    private Long findThirdPartyIdByPhone(String normalizedPhone, String operationType, Long requestId, Long subscriberId) {
        String digits = normalizedPhone.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }

        String sqlFilters = "(t.phone:like:'%" + digits + "%')";
        List<?> response = dolibarrRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/thirdparties")
                .queryParam("limit", 1)
                .queryParam("sortfield", "t.rowid")
                .queryParam("sortorder", "ASC")
                .queryParam("sqlfilters", sqlFilters)
                .build())
            .retrieve()
            .body(List.class);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Object first = response.get(0);
        if (!(first instanceof Map<?, ?> item)) {
            log.warn(
                "Dolibarr validation rejected: operationType={}, requestId={}, subscriberId={}, reason=invalid_thirdparty_payload",
                operationType, requestId, subscriberId
            );
            return null;
        }

        Object idValue = item.get("id");
        if (idValue == null) {
            idValue = item.get("rowid");
        }
        return parseLong(idValue);
    }

    private boolean hasUnpaidInvoices(Long thirdPartyId) {
        List<?> invoices = dolibarrRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/invoices")
                .queryParam("limit", 1)
                .queryParam("thirdparty_ids", thirdPartyId)
                .queryParam("status", "unpaid")
                .build())
            .retrieve()
            .body(List.class);
        return invoices != null && !invoices.isEmpty();
    }

    private boolean resolveTechnicalFailure(
        String operationType,
        Long requestId,
        Long subscriberId,
        String reason,
        Exception ex
    ) {
        boolean decision = !dolibarrProperties.isFailClosed();
        log.warn(
            "Dolibarr validation fallback: operationType={}, requestId={}, subscriberId={}, reason={}, failClosed={}, decision={}",
            operationType, requestId, subscriberId, reason, dolibarrProperties.isFailClosed(), decision, ex
        );
        return decision;
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        String trimmed = phone.trim();
        if (trimmed.isBlank()) {
            return "";
        }
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return "";
        }
        return trimmed.startsWith("+") ? "+" + digits : digits;
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
