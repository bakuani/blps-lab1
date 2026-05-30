package ru.urasha.callmeani.blps.service.eis;

import ru.urasha.callmeani.blps.domain.entity.Subscriber;

public interface DolibarrSubscriberService {

    void syncSubscriber(Subscriber subscriber);

    Long ensureThirdPartyId(Subscriber subscriber);
}
