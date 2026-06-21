package ru.urasha.callmeani.blps.logging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoggingFields {

    public static final String CORRELATION_ID = "correlation.id";
    public static final String EVENT_ACTION = "event.action";
    public static final String EVENT_CATEGORY = "event.category";
    public static final String EVENT_DURATION = "event.duration";
    public static final String EVENT_OUTCOME = "event.outcome";
    public static final String USER_NAME = "user.name";
    public static final String USER_ROLES = "user.roles";
    public static final String HTTP_METHOD = "http.request.method";
    public static final String HTTP_STATUS_CODE = "http.response.status_code";
    public static final String URL_PATH = "url.path";
    public static final String CLIENT_ADDRESS = "client.address";
    public static final String PROCESS_KEY = "process.key";
    public static final String PROCESS_INSTANCE_ID = "process.instance.id";
    public static final String PROCESS_BUSINESS_KEY = "process.business.key";
    public static final String CAMUNDA_TASK_ID = "camunda.task.id";
    public static final String CAMUNDA_TOPIC = "camunda.topic";
    public static final String BUSINESS_OPERATION = "business.operation";
    public static final String BUSINESS_REQUEST_ID = "business.request.id";
    public static final String SUBSCRIBER_ID = "subscriber.id";
}
