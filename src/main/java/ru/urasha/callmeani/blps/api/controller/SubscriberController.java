package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberResponse;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberUpsertRequest;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

@RestController
@RequestMapping("/api/v1/subscribers")
@RequiredArgsConstructor
public class SubscriberController {

    private final SubscriberService subscriberService;

    @GetMapping
    public Page<SubscriberResponse> getSubscribers(Pageable pageable) {
        return subscriberService.getSubscribers(pageable);
    }

    @GetMapping("/{id}")
    public SubscriberResponse getSubscriber(@PathVariable Long id) {
        return subscriberService.getSubscriber(id);
    }

    @PostMapping
    public ResponseEntity<SubscriberResponse> createSubscriber(@Valid @RequestBody SubscriberUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriberService.createSubscriber(request));
    }

    @PutMapping("/{id}")
    public SubscriberResponse updateSubscriber(@PathVariable Long id, @Valid @RequestBody SubscriberUpsertRequest request) {
        return subscriberService.updateSubscriber(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscriber(@PathVariable Long id) {
        subscriberService.deleteSubscriber(id);
        return ResponseEntity.noContent().build();
    }
}

