package ru.urasha.callmeani.blps.eis.dolibarr.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ManagedConnectionFactory;

public class DolibarrConnectionFactoryImpl implements DolibarrConnectionFactory {

    private final ManagedConnectionFactory managedConnectionFactory;
    private final ConnectionManager connectionManager;

    public DolibarrConnectionFactoryImpl(
        ManagedConnectionFactory managedConnectionFactory,
        ConnectionManager connectionManager
    ) {
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
    }

    @Override
    public DolibarrConnection getConnection() {
        try {
            return (DolibarrConnection) connectionManager.allocateConnection(managedConnectionFactory, null);
        } catch (ResourceException ex) {
            throw new DolibarrAuditException("Cannot allocate Dolibarr local JCA-style connection", ex);
        }
    }
}

