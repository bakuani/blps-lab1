package ru.urasha.callmeani.blps.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.urasha.callmeani.blps.api.dto.billing.MonthlyFeeChargeRequestStatusResponse;
import ru.urasha.callmeani.blps.service.billing.async.MonthlyFeeChargeAsyncOperations;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BillingController {

    private final MonthlyFeeChargeAsyncOperations monthlyFeeChargeAsyncService;

    @GetMapping("/subscribers/{subscriberId}/monthly-fee-requests/{requestId}")
    @PreAuthorize("@accessGuard.canAccessSubscriber(authentication, #p0)")
    public MonthlyFeeChargeRequestStatusResponse getMonthlyFeeRequestStatus(
        @PathVariable Long subscriberId,
        @PathVariable Long requestId
    ) {
        return monthlyFeeChargeAsyncService.getStatus(subscriberId, requestId);
    }

    @GetMapping("/subscribers/{subscriberId}/monthly-fee-requests")
    @PreAuthorize("@accessGuard.canAccessSubscriber(authentication, #p0)")
    public List<MonthlyFeeChargeRequestStatusResponse> getRecentMonthlyFeeRequestStatuses(@PathVariable Long subscriberId) {
        return monthlyFeeChargeAsyncService.getRecentStatuses(subscriberId);
    }
}
