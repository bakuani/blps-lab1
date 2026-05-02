package ru.urasha.callmeani.blps.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionConfig {

    @Bean
    @Profile("wildfly")
    @Primary
    public PlatformTransactionManager jtaTransactionManager() {
        JtaTransactionManager transactionManager = new JtaTransactionManager();
        transactionManager.setAutodetectUserTransaction(true);
        transactionManager.setAutodetectTransactionManager(true);
        return transactionManager;
    }

    @Bean(name = "businessTransactionTemplate")
    public TransactionTemplate businessTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
