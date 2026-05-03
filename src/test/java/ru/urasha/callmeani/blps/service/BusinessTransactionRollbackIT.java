package ru.urasha.callmeani.blps.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffRequest;
import ru.urasha.callmeani.blps.domain.entity.AdditionalFeature;
import ru.urasha.callmeani.blps.domain.entity.FeatureCategory;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.SubscriberFeature;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffCategory;
import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;
import ru.urasha.callmeani.blps.repository.AdditionalFeatureRepository;
import ru.urasha.callmeani.blps.repository.BillingTransactionRepository;
import ru.urasha.callmeani.blps.repository.FeatureCategoryRepository;
import ru.urasha.callmeani.blps.repository.NotificationEventRepository;
import ru.urasha.callmeani.blps.repository.SubscriberFeatureRepository;
import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.repository.TariffCategoryRepository;
import ru.urasha.callmeani.blps.repository.TariffRepository;
import ru.urasha.callmeani.blps.service.feature.FeatureManagementService;
import ru.urasha.callmeani.blps.service.notification.NotificationService;
import ru.urasha.callmeani.blps.service.tariff.TariffManagementService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:tx-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=01234567890123456789012345678901"
    }
)
class BusinessTransactionRollbackIT {

    @Autowired
    private TariffManagementService tariffManagementService;
    @Autowired
    private FeatureManagementService featureManagementService;

    @Autowired
    private TariffCategoryRepository tariffCategoryRepository;
    @Autowired
    private TariffRepository tariffRepository;
    @Autowired
    private SubscriberRepository subscriberRepository;
    @Autowired
    private FeatureCategoryRepository featureCategoryRepository;
    @Autowired
    private AdditionalFeatureRepository additionalFeatureRepository;
    @Autowired
    private SubscriberFeatureRepository subscriberFeatureRepository;
    @Autowired
    private BillingTransactionRepository billingTransactionRepository;
    @Autowired
    private NotificationEventRepository notificationEventRepository;

    @SpyBean
    private NotificationService notificationService;

    @BeforeEach
    void cleanDb() {
        billingTransactionRepository.deleteAll();
        notificationEventRepository.deleteAll();
        subscriberFeatureRepository.deleteAll();
        subscriberRepository.deleteAll();
        additionalFeatureRepository.deleteAll();
        featureCategoryRepository.deleteAll();
        tariffRepository.deleteAll();
        tariffCategoryRepository.deleteAll();
    }

    @AfterEach
    void resetSpies() {
        reset(notificationService);
    }

    @Test
    void changeTariffRollsBackAllWritesWhenFinalNotificationFails() {
        TariffCategory category = new TariffCategory();
        category.setName("Base");
        category = tariffCategoryRepository.save(category);

        Tariff oldTariff = createTariff("Old", new BigDecimal("100.00"), new BigDecimal("0.00"), category);
        Tariff newTariff = createTariff("New", new BigDecimal("200.00"), new BigDecimal("50.00"), category);

        Subscriber subscriber = new Subscriber();
        subscriber.setPhone("79990000001");
        subscriber.setFullName("Rollback User");
        subscriber.setBalance(new BigDecimal("1000.00"));
        subscriber.setCurrentTariff(oldTariff);
        subscriber = subscriberRepository.save(subscriber);

        doThrow(new RuntimeException("fail notification"))
            .when(notificationService)
            .createNotification(any(), any(), anyString(), anyBoolean());

        Long subscriberId = subscriber.getId();
        Long targetTariffId = newTariff.getId();

        assertThatThrownBy(() -> tariffManagementService.changeTariff(
            subscriberId,
            new ChangeTariffRequest(targetTariffId, Map.of())
        )).isInstanceOf(RuntimeException.class);

        Subscriber reloaded = subscriberRepository.findById(subscriberId).orElseThrow();
        assertThat(reloaded.getCurrentTariff().getId()).isEqualTo(oldTariff.getId());
        assertThat(reloaded.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(billingTransactionRepository.count()).isZero();
        assertThat(notificationEventRepository.count()).isZero();
    }

    @Test
    void disableFeatureRollsBackStatusAndBillingWhenFinalNotificationFails() {
        FeatureCategory category = new FeatureCategory();
        category.setName("Fun");
        category = featureCategoryRepository.save(category);

        AdditionalFeature feature = new AdditionalFeature();
        feature.setName("Music");
        feature.setDescription("Music feature");
        feature.setMonthlyFee(new BigDecimal("99.00"));
        feature.setCategory(category);
        feature = additionalFeatureRepository.save(feature);

        Subscriber subscriber = new Subscriber();
        subscriber.setPhone("79990000002");
        subscriber.setFullName("Disable Rollback User");
        subscriber.setBalance(new BigDecimal("500.00"));
        subscriber = subscriberRepository.save(subscriber);

        SubscriberFeature subscriberFeature = new SubscriberFeature();
        subscriberFeature.setSubscriber(subscriber);
        subscriberFeature.setFeature(feature);
        subscriberFeature.setStatus(SubscriberFeatureStatus.ACTIVE);
        subscriberFeature.setConnectedAt(OffsetDateTime.now());
        subscriberFeature = subscriberFeatureRepository.save(subscriberFeature);

        doThrow(new RuntimeException("fail notification"))
            .when(notificationService)
            .createNotification(any(), any(), anyString(), anyBoolean());

        Long subscriberId = subscriber.getId();
        Long featureId = feature.getId();

        assertThatThrownBy(() -> featureManagementService.disableFeature(subscriberId, featureId))
            .isInstanceOf(RuntimeException.class);

        SubscriberFeature reloaded = subscriberFeatureRepository.findById(subscriberFeature.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubscriberFeatureStatus.ACTIVE);
        assertThat(reloaded.getDisabledAt()).isNull();
        assertThat(billingTransactionRepository.count()).isZero();
        assertThat(notificationEventRepository.count()).isZero();
    }

    private Tariff createTariff(String name, BigDecimal monthlyFee, BigDecimal switchFee, TariffCategory category) {
        Tariff tariff = new Tariff();
        tariff.setName(name);
        tariff.setDescription(name + " tariff");
        tariff.setMonthlyFee(monthlyFee);
        tariff.setSwitchFee(switchFee);
        tariff.setCustomizable(false);
        tariff.setPdfUrl("https://example.com/" + name.toLowerCase() + ".pdf");
        tariff.setCategory(category);
        return tariffRepository.save(tariff);
    }
}
