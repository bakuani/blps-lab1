package ru.urasha.callmeani.blps.eis.dolibarr.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.io.Serial;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

public class DolibarrManagedConnectionFactory implements ManagedConnectionFactory {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String baseUrl;
    private final String apiKey;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private PrintWriter logWriter;

    public DolibarrManagedConnectionFactory(
        String baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout
    ) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    @Override
    public Object createConnectionFactory(ConnectionManager connectionManager) {
        return new DolibarrConnectionFactoryImpl(this, connectionManager);
    }

    @Override
    public Object createConnectionFactory() {
        return new DolibarrConnectionFactoryImpl(this, new DolibarrLocalConnectionManager());
    }

    @Override
    public ManagedConnection createManagedConnection(
        Subject subject,
        ConnectionRequestInfo connectionRequestInfo
    ) throws ResourceException {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ResourceException("Dolibarr baseUrl is not configured");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResourceException("Dolibarr apiKey is not configured");
        }
        return new DolibarrManagedConnection(baseUrl, apiKey, connectTimeout, readTimeout);
    }

    @Override
    public ManagedConnection matchManagedConnections(
        Set connectionSet,
        Subject subject,
        ConnectionRequestInfo connectionRequestInfo
    ) {
        if (connectionSet == null || connectionSet.isEmpty()) {
            return null;
        }
        for (Object candidate : connectionSet) {
            if (candidate instanceof DolibarrManagedConnection managedConnection) {
                return managedConnection;
            }
        }
        return null;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseUrl, maskedApiKey(), connectTimeout, readTimeout);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DolibarrManagedConnectionFactory that)) {
            return false;
        }
        return Objects.equals(baseUrl, that.baseUrl)
            && Objects.equals(maskedApiKey(), that.maskedApiKey())
            && Objects.equals(connectTimeout, that.connectTimeout)
            && Objects.equals(readTimeout, that.readTimeout);
    }

    private String maskedApiKey() {
        if (apiKey == null) {
            return null;
        }
        return BigInteger.valueOf(apiKey.hashCode()).toString(16);
    }

    private String normalizeBaseUrl(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String normalized = rawUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

