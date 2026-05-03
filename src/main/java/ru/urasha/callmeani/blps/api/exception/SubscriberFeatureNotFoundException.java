package ru.urasha.callmeani.blps.api.exception;

public class SubscriberFeatureNotFoundException extends NotFoundException {

    public SubscriberFeatureNotFoundException(Long id) {
        super("Subscriber feature not found: " + id);
    }
}

