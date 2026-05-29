package ru.urasha.callmeani.blps.service.eis.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import ru.urasha.callmeani.blps.config.DolibarrProperties;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.TariffChangeRequest;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class DolibarrValidationServiceImplTest {

    @Mock
    private SubscriberService subscriberService;

    private MockRestServiceServer server;
    private DolibarrValidationServiceImpl validationService;

    @BeforeEach
    void setUp() {
        DolibarrProperties properties = new DolibarrProperties();
        properties.setUrl("http://localhost:18081");
        properties.setApiKey("test_api_key");
        properties.setFailClosed(true);
        properties.setConnectTimeoutMs(3000);
        properties.setReadTimeoutMs(5000);

        RestClient.Builder builder = RestClient.builder()
            .baseUrl("http://localhost:18081/api/index.php")
            .defaultHeader("DOLAPIKEY", "test_api_key");
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        validationService = new DolibarrValidationServiceImpl(subscriberService, restClient, properties);
    }

    @Test
    void allowTariffChangeReturnsTrueWhenClientExistsAndNoUnpaidInvoices() {
        TariffChangeRequest request = tariffChangeRequest(10L, 100L);
        when(subscriberService.getSubscriberEntity(10L)).thenReturn(subscriber("+7 (999) 000-00-00"));

        server.expect(once(), requestTo(containsString("/api/index.php/thirdparties")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("DOLAPIKEY", "test_api_key"))
            .andRespond(withSuccess("[{\"id\":501,\"phone\":\"+79990000000\"}]", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(containsString("/api/index.php/invoices")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("DOLAPIKEY", "test_api_key"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        boolean allowed = validationService.allowTariffChange(request);

        assertThat(allowed).isTrue();
        server.verify();
    }

    @Test
    void allowTariffChangeReturnsFalseWhenClientHasUnpaidInvoices() {
        TariffChangeRequest request = tariffChangeRequest(11L, 101L);
        when(subscriberService.getSubscriberEntity(11L)).thenReturn(subscriber("+7 (999) 111-11-11"));

        server.expect(once(), requestTo(containsString("/api/index.php/thirdparties")))
            .andRespond(withSuccess("[{\"id\":777,\"phone\":\"+79991111111\"}]", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(containsString("/api/index.php/invoices")))
            .andRespond(withSuccess("[{\"id\":1001,\"status\":\"unpaid\"}]", MediaType.APPLICATION_JSON));

        boolean allowed = validationService.allowTariffChange(request);

        assertThat(allowed).isFalse();
        server.verify();
    }

    @Test
    void allowTariffChangeReturnsFalseWhenClientNotFoundInDolibarr() {
        TariffChangeRequest request = tariffChangeRequest(12L, 102L);
        when(subscriberService.getSubscriberEntity(12L)).thenReturn(subscriber("+7 (999) 222-22-22"));

        server.expect(once(), requestTo(containsString("/api/index.php/thirdparties")))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        boolean allowed = validationService.allowTariffChange(request);

        assertThat(allowed).isFalse();
        server.verify();
    }

    @Test
    void allowTariffChangeReturnsFalseOnUnauthorizedResponse() {
        TariffChangeRequest request = tariffChangeRequest(13L, 103L);
        when(subscriberService.getSubscriberEntity(13L)).thenReturn(subscriber("+7 (999) 333-33-33"));

        server.expect(once(), requestTo(containsString("/api/index.php/thirdparties")))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON));

        boolean allowed = validationService.allowTariffChange(request);

        assertThat(allowed).isFalse();
        server.verify();
    }

    @Test
    void allowTariffChangeReturnsFalseOnConnectionError() {
        TariffChangeRequest request = tariffChangeRequest(14L, 104L);
        when(subscriberService.getSubscriberEntity(14L)).thenReturn(subscriber("+7 (999) 444-44-44"));

        server.expect(once(), requestTo(containsString("/api/index.php/thirdparties")))
            .andRespond(httpRequest -> {
                throw new ResourceAccessException("connection refused");
            });

        boolean allowed = validationService.allowTariffChange(request);

        assertThat(allowed).isFalse();
        server.verify();
    }

    private TariffChangeRequest tariffChangeRequest(Long subscriberId, Long requestId) {
        TariffChangeRequest request = new TariffChangeRequest();
        request.setId(requestId);
        request.setSubscriberId(subscriberId);
        return request;
    }

    private Subscriber subscriber(String phone) {
        Subscriber subscriber = new Subscriber();
        subscriber.setPhone(phone);
        return subscriber;
    }
}
