package ru.urasha.callmeani.blps.api.exception;

public class SubscriberNotFoundException extends NotFoundException {

    public SubscriberNotFoundException(Long id) {
        super("Subscriber not found: " + id);
    }
}

