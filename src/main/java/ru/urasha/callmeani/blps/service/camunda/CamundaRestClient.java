package ru.urasha.callmeani.blps.service.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CamundaRestClient {

    private final RestClient camundaRestClient;
    private final CamundaProperties properties;

    public String startProcess(String processDefinitionKey, String businessKey, Map<String, CamundaVariable> variables) {
        ProcessStartResponse response = camundaRestClient.post()
            .uri("/process-definition/key/{key}/start", processDefinitionKey)
            .body(new ProcessStartRequest(businessKey, variables))
            .retrieve()
            .body(ProcessStartResponse.class);

        if (response == null || response.id() == null) {
            throw new IllegalStateException("Camunda did not return a process instance id");
        }
        return response.id();
    }

    public void completeFirstTask(String processInstanceId, Map<String, CamundaVariable> variables) {
        List<TaskResponse> tasks = camundaRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/task")
                .queryParam("processInstanceId", processInstanceId)
            .queryParam("maxResults", 1)
                .build())
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });

        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        camundaRestClient.post()
            .uri("/task/{id}/complete", tasks.get(0).id())
            .body(new CompleteTaskRequest(variables))
            .retrieve()
            .toBodilessEntity();
    }

    public List<LockedExternalTask> fetchAndLock(List<String> topics) {
        List<TopicFetchRequest> topicRequests = topics.stream()
            .map(topic -> new TopicFetchRequest(topic, properties.getLockDurationMs()))
            .toList();

        List<LockedExternalTask> tasks = camundaRestClient.post()
            .uri("/external-task/fetchAndLock")
            .body(new FetchAndLockRequest(
                properties.getWorkerId(),
                properties.getMaxTasks(),
                true,
                topicRequests
            ))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });

        return tasks == null ? List.of() : tasks;
    }

    public void completeExternalTask(String taskId, Map<String, CamundaVariable> variables) {
        camundaRestClient.post()
            .uri("/external-task/{id}/complete", taskId)
            .body(new CompleteExternalTaskRequest(properties.getWorkerId(), variables))
            .retrieve()
            .toBodilessEntity();
    }

    public void handleFailure(String taskId, String message, String details, int retries) {
        camundaRestClient.post()
            .uri("/external-task/{id}/failure", taskId)
            .body(new ExternalTaskFailureRequest(
                properties.getWorkerId(),
                message,
                details,
                retries,
                properties.getRetryTimeoutMs()
            ))
            .retrieve()
            .toBodilessEntity();
    }

    private record ProcessStartRequest(String businessKey, Map<String, CamundaVariable> variables) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProcessStartResponse(String id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TaskResponse(String id) {
    }

    private record CompleteTaskRequest(Map<String, CamundaVariable> variables) {
    }

    private record FetchAndLockRequest(
        String workerId,
        int maxTasks,
        boolean usePriority,
        List<TopicFetchRequest> topics
    ) {
    }

    private record TopicFetchRequest(String topicName, long lockDuration) {
    }

    private record CompleteExternalTaskRequest(String workerId, Map<String, CamundaVariable> variables) {
    }

    private record ExternalTaskFailureRequest(
        String workerId,
        String errorMessage,
        String errorDetails,
        int retries,
        long retryTimeout
    ) {
    }
}
