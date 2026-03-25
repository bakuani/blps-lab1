package ru.urasha.callmeani.blps.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.urasha.callmeani.blps.domain.entity.AdditionalService;
import ru.urasha.callmeani.blps.domain.entity.ServiceCategory;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.SubscriberService;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffCategory;
import ru.urasha.callmeani.blps.domain.entity.TariffOption;
import ru.urasha.callmeani.blps.domain.enums.SubscriberServiceStatus;
import ru.urasha.callmeani.blps.repository.AdditionalServiceRepository;
import ru.urasha.callmeani.blps.repository.ServiceCategoryRepository;
import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.repository.SubscriberServiceRepository;
import ru.urasha.callmeani.blps.repository.TariffCategoryRepository;
import ru.urasha.callmeani.blps.repository.TariffRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Configuration
public class DemoDataInitializer {

    @Bean
    CommandLineRunner seedData(
        TariffCategoryRepository tariffCategoryRepository,
        TariffRepository tariffRepository,
        ServiceCategoryRepository serviceCategoryRepository,
        AdditionalServiceRepository additionalServiceRepository,
        SubscriberRepository subscriberRepository,
        SubscriberServiceRepository subscriberServiceRepository
    ) {
        return args -> {
            if (subscriberRepository.count() > 0) {
                return;
            }

            TariffCategory baseCategory = new TariffCategory();
            baseCategory.setName("Базовые");
            TariffCategory premiumCategory = new TariffCategory();
            premiumCategory.setName("Премиум");
            tariffCategoryRepository.saveAll(List.of(baseCategory, premiumCategory));

            Tariff smart = new Tariff();
            smart.setName("Smart");
            smart.setDescription("Базовый тариф для звонков и интернета");
            smart.setMonthlyFee(new BigDecimal("650.00"));
            smart.setSwitchFee(new BigDecimal("100.00"));
            smart.setCustomizable(false);
            smart.setPdfUrl("https://mts.ru/tariffs/smart.pdf");
            smart.setCategory(baseCategory);

            Tariff myTariff = new Tariff();
            myTariff.setName("Мой Тариф");
            myTariff.setDescription("Кастомизируемый тариф");
            myTariff.setMonthlyFee(new BigDecimal("900.00"));
            myTariff.setSwitchFee(new BigDecimal("0.00"));
            myTariff.setCustomizable(true);
            myTariff.setPdfUrl("https://mts.ru/tariffs/my-tariff.pdf");
            myTariff.setCategory(premiumCategory);

            TariffOption minutes = new TariffOption();
            minutes.setName("Пакет минут +200");
            minutes.setDescription("Дополнительные 200 минут");
            minutes.setPrice(new BigDecimal("150.00"));
            minutes.setTariff(myTariff);

            TariffOption traffic = new TariffOption();
            traffic.setName("Интернет +20 ГБ");
            traffic.setDescription("Дополнительный интернет-пакет");
            traffic.setPrice(new BigDecimal("220.00"));
            traffic.setTariff(myTariff);

            myTariff.getOptions().add(minutes);
            myTariff.getOptions().add(traffic);

            tariffRepository.saveAll(List.of(smart, myTariff));

            ServiceCategory safety = new ServiceCategory();
            safety.setName("Безопасность");
            ServiceCategory entertainment = new ServiceCategory();
            entertainment.setName("Развлечения");
            serviceCategoryRepository.saveAll(List.of(safety, entertainment));

            AdditionalService blocker = new AdditionalService();
            blocker.setName("Блокировка спама");
            blocker.setDescription("Фильтрация нежелательных звонков");
            blocker.setMonthlyFee(new BigDecimal("99.00"));
            blocker.setCategory(safety);

            AdditionalService music = new AdditionalService();
            music.setName("МТС Music");
            music.setDescription("Подписка на музыкальный сервис");
            music.setMonthlyFee(new BigDecimal("169.00"));
            music.setCategory(entertainment);

            additionalServiceRepository.saveAll(List.of(blocker, music));

            Subscriber subscriber = new Subscriber();
            subscriber.setPhone("+79990000000");
            subscriber.setFullName("Тестовый Абонент");
            subscriber.setBalance(new BigDecimal("3000.00"));
            subscriber.setCurrentTariff(smart);
            subscriberRepository.save(subscriber);

            SubscriberService s1 = new SubscriberService();
            s1.setSubscriber(subscriber);
            s1.setService(blocker);
            s1.setStatus(SubscriberServiceStatus.ACTIVE);
            s1.setConnectedAt(OffsetDateTime.now().minusMonths(2));

            SubscriberService s2 = new SubscriberService();
            s2.setSubscriber(subscriber);
            s2.setService(music);
            s2.setStatus(SubscriberServiceStatus.ACTIVE);
            s2.setConnectedAt(OffsetDateTime.now().minusMonths(1));

            subscriberServiceRepository.saveAll(List.of(s1, s2));
        };
    }
}
