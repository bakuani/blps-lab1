package ru.urasha.callmeani.blps.service.eis.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.urasha.callmeani.blps.config.DolibarrProperties;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.service.eis.DolibarrSubscriberService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DolibarrSubscriberSyncService implements DolibarrSubscriberService {

    @Qualifier("dolibarrRestClient")
    private final RestClient dolibarrRestClient;
    private final DolibarrProperties dolibarrProperties;

    @Override
    public void syncSubscriber(Subscriber subscriber) {
        if (!dolibarrProperties.isSubscriberSyncEnabled()) {
            return;
        }
        ensureThirdPartyId(subscriber);
    }

    @Override
    public Long ensureThirdPartyId(Subscriber subscriber) {
        if (subscriber == null) {
            return null;
        }

        String normalizedPhone = normalizePhone(subscriber.getPhone());
        if (normalizedPhone.isBlank()) {
            log.warn(
                "Dolibarr subscriber sync skipped: subscriberId={}, reason=empty_phone",
                subscriber.getId()
            );
            return null;
        }

        try {
            Long thirdPartyId = findThirdPartyIdByPhone(normalizedPhone);
            Map<String, Object> payload = buildThirdPartyPayload(subscriber, normalizedPhone);
            if (thirdPartyId == null) {
                Long createdId = createThirdParty(payload);
                log.info(
                    "Dolibarr subscriber sync created: subscriberId={}, thirdPartyId={}, phone={}",
                    subscriber.getId(), createdId, normalizedPhone
                );
                return createdId;
            }

            updateThirdParty(thirdPartyId, payload);
            log.info(
                "Dolibarr subscriber sync updated: subscriberId={}, thirdPartyId={}, phone={}",
                subscriber.getId(), thirdPartyId, normalizedPhone
            );
            return thirdPartyId;
        } catch (RestClientResponseException ex) {
            log.warn(
                "Dolibarr subscriber sync failed: subscriberId={}, reason=http_status_{}, body={}",
                subscriber.getId(),
                ex.getStatusCode().value(),
                ex.getResponseBodyAsString()
            );
            return null;
        } catch (RestClientException ex) {
            log.warn(
                "Dolibarr subscriber sync failed: subscriberId={}, reason=network_error, message={}",
                subscriber.getId(),
                ex.getMessage()
            );
            return null;
        } catch (RuntimeException ex) {
            log.warn(
                "Dolibarr subscriber sync failed: subscriberId={}, reason=unexpected_error, message={}",
                subscriber.getId(),
                ex.getMessage()
            );
            return null;
        }
    }

    private Long findThirdPartyIdByPhone(String normalizedPhone) {
        String digits = normalizedPhone.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }

        String sqlFilters = "(t.phone:like:'%" + digits + "%')";
        try {
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
                return null;
            }
            Object idValue = item.get("id");
            if (idValue == null) {
                idValue = item.get("rowid");
            }
            return parseLong(idValue);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                return null;
            }
            throw ex;
        }
    }

    private Long createThirdParty(Map<String, Object> payload) {
        Object response = dolibarrRestClient.post()
            .uri("/thirdparties")
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

    private void updateThirdParty(Long thirdPartyId, Map<String, Object> payload) {
        dolibarrRestClient.put()
            .uri("/thirdparties/{id}", thirdPartyId)
            .body(payload)
            .retrieve()
            .toBodilessEntity();
    }

    private Map<String, Object> buildThirdPartyPayload(Subscriber subscriber, String normalizedPhone) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", subscriber.getFullName());
        payload.put("phone", normalizedPhone);
        payload.put("client", 1);
        return payload;
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
