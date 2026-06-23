package ru.urasha.callmeani.blps.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.urasha.callmeani.blps.domain.entity.FeatureDisableRequest;
import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;
import ru.urasha.callmeani.blps.repository.FeatureDisableRequestRepository;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
public class DevController {

    private final FeatureDisableRequestRepository featureDisableRequestRepository;

    @PostMapping("/simulate-stuck-request")
    public Long simulateStuckRequest() {
        FeatureDisableRequest request = new FeatureDisableRequest();
        request.setSubscriberId(1L);
        request.setFeatureId(1L);
        
        request.setStatus(BusinessRequestStatus.RETRY);
        request.setAttemptCount(1);
        request.setCreatedAt(OffsetDateTime.now().minusMinutes(2));
        request.setUpdatedAt(OffsetDateTime.now().minusMinutes(2));
        
        return featureDisableRequestRepository.save(request).getId();
    }
}
