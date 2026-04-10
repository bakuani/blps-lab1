package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberUpsertRequest;
import ru.urasha.callmeani.blps.service.AdminSubscriberService;

@RestController
@RequestMapping("/api/v1/admin/subscribers")
@RequiredArgsConstructor
public class AdminSubscriberController {

    private final AdminSubscriberService AdminSubscriberService;

    @GetMapping
    public Page<SubscriberAdminResponse> getSubscribers(Pageable pageable) {
        return AdminSubscriberService.getSubscribers(pageable);
    }

    @GetMapping("/{id}")
    public SubscriberAdminResponse getSubscriber(@PathVariable Long id) {
        return AdminSubscriberService.getSubscriber(id);
    }

    @PostMapping
    public ResponseEntity<SubscriberAdminResponse> createSubscriber(@Valid @RequestBody SubscriberUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminSubscriberService.createSubscriber(request));
    }

    @PutMapping("/{id}")
    public SubscriberAdminResponse updateSubscriber(@PathVariable Long id, @Valid @RequestBody SubscriberUpsertRequest request) {
        return AdminSubscriberService.updateSubscriber(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscriber(@PathVariable Long id) {
        AdminSubscriberService.deleteSubscriber(id);
        return ResponseEntity.noContent().build();
    }
}


