package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberResponse;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberUpsertRequest;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

@RestController
@RequestMapping("/api/v1/admin/subscribers")
@RequiredArgsConstructor
public class SubscriberManagementController {

    private final SubscriberService SubscriberService;

    @GetMapping
    public Page<SubscriberResponse> getSubscribers(Pageable pageable) {
        return SubscriberService.getSubscribers(pageable);
    }

    @GetMapping("/{id}")
    public SubscriberResponse getSubscriber(@PathVariable Long id) {
        return SubscriberService.getSubscriber(id);
    }

    @PostMapping
    public ResponseEntity<SubscriberResponse> createSubscriber(@Valid @RequestBody SubscriberUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriberService.createSubscriber(request));
    }

    @PutMapping("/{id}")
    public SubscriberResponse updateSubscriber(@PathVariable Long id, @Valid @RequestBody SubscriberUpsertRequest request) {
        return SubscriberService.updateSubscriber(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscriber(@PathVariable Long id) {
        SubscriberService.deleteSubscriber(id);
        return ResponseEntity.noContent().build();
    }
}
