package ru.urasha.callmeani.blps.eis.dolibarr.ra;

import java.time.Duration;

public class DolibarrConnection implements AutoCloseable {

    private final String baseUrl;
    private final String apiKey;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private boolean closed;

    public DolibarrConnection(
        String baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout
    ) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public DolibarrInteraction createInteraction() {
        if (closed) {
            throw new DolibarrAuditException("Dolibarr connection is closed");
        }
        return new DolibarrInteraction(baseUrl, apiKey, connectTimeout, readTimeout);
    }

    @Override
    public void close() {
        this.closed = true;
    }
}

