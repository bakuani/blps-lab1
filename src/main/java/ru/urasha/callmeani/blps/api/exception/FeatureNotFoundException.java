package ru.urasha.callmeani.blps.api.exception;

public class FeatureNotFoundException extends NotFoundException {

    public FeatureNotFoundException(Long id) {
        super("Feature not found: " + id);
    }
}

