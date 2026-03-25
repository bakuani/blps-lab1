package ru.urasha.callmeani.blps.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.urasha.callmeani.blps.api.dto.DisableServiceResponse;
import ru.urasha.callmeani.blps.api.dto.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.ServiceDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.ServiceSummaryDto;
import ru.urasha.callmeani.blps.service.ServiceManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    @GetMapping("/subscribers/{subscriberId}/services")
    public List<ServiceSummaryDto> findSubscriberServices(
        @PathVariable Long subscriberId,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String query
    ) {
        return serviceManagementService.findSubscriberServices(subscriberId, categoryId, query);
    }

    @GetMapping("/services/categories")
    public List<IdNameDto> getServiceCategories() {
        return serviceManagementService.getServiceCategories();
    }

    @GetMapping("/services/{serviceId}")
    public ServiceDetailsResponse getServiceDetails(@PathVariable Long serviceId) {
        return serviceManagementService.getServiceDetails(serviceId);
    }

    @PostMapping("/subscribers/{subscriberId}/services/{serviceId}/disable")
    public DisableServiceResponse disableService(@PathVariable Long subscriberId, @PathVariable Long serviceId) {
        return serviceManagementService.disableService(subscriberId, serviceId);
    }
}
