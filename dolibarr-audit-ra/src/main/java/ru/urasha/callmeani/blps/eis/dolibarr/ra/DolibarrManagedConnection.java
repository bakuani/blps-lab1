package ru.urasha.callmeani.blps.eis.dolibarr.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;

import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.time.Duration;

public class DolibarrManagedConnection implements ManagedConnection {

    private final String baseUrl;
    private final String apiKey;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private PrintWriter logWriter;

    public DolibarrManagedConnection(
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

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) {
        return new DolibarrConnection(baseUrl, apiKey, connectTimeout, readTimeout);
    }

    @Override
    public void destroy() {
        // No pooled physical state in local mode.
    }

    @Override
    public void cleanup() {
        // No pooled physical state in local mode.
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (!(connection instanceof DolibarrConnection)) {
            throw new ResourceException("Unsupported Dolibarr connection handle");
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        // No connection events in local mode.
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        // No connection events in local mode.
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new ResourceException("Dolibarr local JCA-style adapter does not support XA transactions");
    }

    @Override
    public LocalTransaction getLocalTransaction() {
        return new LocalTransaction() {
            @Override
            public void begin() {
                // No-op local transaction.
            }

            @Override
            public void commit() {
                // No-op local transaction.
            }

            @Override
            public void rollback() {
                // No-op local transaction.
            }
        };
    }

    @Override
    public ManagedConnectionMetaData getMetaData() {
        return new ManagedConnectionMetaData() {
            @Override
            public String getEISProductName() {
                return "Dolibarr ERP/CRM";
            }

            @Override
            public String getEISProductVersion() {
                return "REST API";
            }

            @Override
            public int getMaxConnections() {
                return 0;
            }

            @Override
            public String getUserName() {
                return BigInteger.valueOf(apiKey.hashCode()).toString(16);
            }
        };
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }
}

