package ru.urasha.callmeani.blps.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.urasha.callmeani.blps.eis.dolibarr.ra.DolibarrConnectionFactory;
import ru.urasha.callmeani.blps.eis.dolibarr.ra.DolibarrConnectionFactoryImpl;
import ru.urasha.callmeani.blps.eis.dolibarr.ra.DolibarrLocalConnectionManager;
import ru.urasha.callmeani.blps.eis.dolibarr.ra.DolibarrManagedConnectionFactory;

import java.time.Duration;

@Configuration
public class DolibarrAuditConnectionConfig {

    @Bean
    public DolibarrConnectionFactory dolibarrAuditConnectionFactory(DolibarrProperties properties) {
        DolibarrManagedConnectionFactory managedConnectionFactory = new DolibarrManagedConnectionFactory(
            properties.getUrl(),
            properties.getApiKey(),
            Duration.ofMillis(properties.getConnectTimeoutMs()),
            Duration.ofMillis(properties.getReadTimeoutMs())
        );
        return new DolibarrConnectionFactoryImpl(managedConnectionFactory, new DolibarrLocalConnectionManager());
    }
}

